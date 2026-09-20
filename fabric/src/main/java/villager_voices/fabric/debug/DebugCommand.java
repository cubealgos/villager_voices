package villager_voices.fabric.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import villager_voices.LineCatalogue;
import villager_voices.LineRef;
import villager_voices.LineSink;
import villager_voices.ReactionRules;
import villager_voices.VillagerEventBus;
import villager_voices.VillagerReactionEvent;
import villager_voices.VillagerReactionSignal;
import villager_voices.catalogue.Catalogue;
import villager_voices.catalogue.Line;
import villager_voices.display.DisplayLine;
import villager_voices.display.DisplayQueue;
import villager_voices.fabric.VillagerVoicesFabric;
import villager_voices.fabric.catalogue.CatalogueReloadListener;
import villager_voices.fabric.display.ActionBarDisplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Development-only: {@code /villager_voices debug <trigger|lines|events> <...>}
 * (docs/spec/operations/testing.md's "Development tool" row, {@code VV-13}) -- genuinely useful
 * here since several of the 16 {@link VillagerReactionEvent}s (raid, zombification, breeding) are
 * slow or awkward to trigger manually while testing.
 *
 * <p>{@code trigger <event> <villager>} constructs a {@link VillagerReactionSignal} for the named
 * event on the targeted villager and publishes it on a throwaway {@link VillagerEventBus} wired to
 * the real, currently-loaded {@link Catalogue} (VV-3) and the mod's real, currently-live
 * {@link VillagerVoicesFabric#displayQueue()} (VV-7/VV-8), through
 * {@link VillagerEventBus#publishBypassingRules} -- so the per-event and per-villager-global
 * cooldowns never block the forced event, but selection (VV-2's {@code LineSelector}), the
 * no-immediate-repeat rule, the sleep/baby silence rule, and the action-bar display all run exactly
 * as they do in production, not a hardcoded test line. Prints the selected line's subtitle text and
 * sound id, or why none was selected ("no line": the catalogue has no eligible line for the event;
 * "silenced": the villager is asleep or a baby and the event is not that state's one exempt event).
 *
 * <p>{@code lines <event>} prints every line the currently-loaded catalogue holds for one event,
 * read-only. {@code events} lists all 16 event ids this command (and the catalogue's JSON files)
 * accept.
 *
 * <p>Registered only when Fabric reports a development environment (the one guarded line in
 * {@link VillagerVoicesFabric#onInitialize()}); never present in a released jar.
 */
public final class DebugCommand {

    /** Every event id this command accepts, keyed exactly as {@link Catalogue} keys its lines: the {@link VillagerReactionEvent} constant's own name, lower-cased (docs/spec/domains/reaction.md §3's event names, {@code villager_voices.catalogue.Catalogue}'s own mapping note). */
    private static final Map<String, VillagerReactionEvent> EVENTS_BY_ID = buildEventsById();

    private static final SimpleCommandExceptionType NOT_A_VILLAGER =
        new SimpleCommandExceptionType(Component.translatable("command.villager_voices.debug.not_a_villager"));
    private static final Dynamic2CommandExceptionType UNKNOWN_EVENT = new Dynamic2CommandExceptionType(
        (found, valid) -> Component.translatable("command.villager_voices.debug.unknown_event", found, valid));

    private DebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal(VillagerVoicesFabric.MOD_ID).then(Commands.literal("debug")
                .then(Commands.literal("trigger")
                    .then(Commands.argument("event", StringArgumentType.word()).suggests(DebugCommand::suggestEvents)
                        .then(Commands.argument("villager", EntityArgument.entity())
                            .executes(c -> trigger(c.getSource(), eventOf(c), villagerOf(c))))))
                .then(Commands.literal("lines")
                    .then(Commands.argument("event", StringArgumentType.word()).suggests(DebugCommand::suggestEvents)
                        .executes(c -> lines(c.getSource(), eventOf(c)))))
                .then(Commands.literal("events")
                    .executes(c -> events(c.getSource()))))));
    }

    /**
     * Forces {@code event} on {@code villager}: real selection and display, cooldowns bypassed
     * ({@link VillagerEventBus#publishBypassingRules}). Returns {@code 1} when a line was selected,
     * {@code 0} for "silenced" or "no line" -- brigadier's own success/failure convention, not a
     * command-syntax error, since every one of the three outcomes is a legitimate answer for a
     * forced event.
     */
    static int trigger(CommandSourceStack source, VillagerReactionEvent event, Villager villager) {
        ServerLevel level = source.getLevel();
        boolean asleep = villager.isSleeping();
        boolean baby = villager.isBaby();
        Set<UUID> recipients = ActionBarDisplay.playersInHearingRange(level, villager.position()).stream()
            .map(ServerPlayer::getUUID)
            .collect(Collectors.toUnmodifiableSet());
        VillagerReactionSignal signal = new VillagerReactionSignal(villager.getUUID(), event, asleep, baby, recipients);

        Catalogue catalogue = CatalogueReloadListener.current();
        CapturingSink sink = new CapturingSink(catalogue, VillagerVoicesFabric.displayQueue(), villager.getDisplayName().getString());
        VillagerEventBus bus = new VillagerEventBus(new CatalogueAdapter(catalogue), sink, System::currentTimeMillis, new Random()::nextInt);
        bus.publishBypassingRules(signal);

        if (sink.captured != null) {
            Line line = sink.captured;
            source.sendSuccess(() -> Component.translatable(
                "command.villager_voices.debug.trigger.selected", villager.getDisplayName(), line.text(), line.soundId()), false);
            return 1;
        }
        if (ReactionRules.isSilenced(event, asleep, baby)) {
            String reason = silenceReason(event, asleep, baby);
            source.sendSuccess(() -> Component.translatable(
                "command.villager_voices.debug.trigger.silenced", villager.getDisplayName(), reason), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
            "command.villager_voices.debug.trigger.no_line", villager.getDisplayName(), idOf(event)), false);
        return 0;
    }

    /** Prints every line the currently-loaded catalogue holds for {@code event}, in catalogue order. */
    static int lines(CommandSourceStack source, VillagerReactionEvent event) {
        List<Line> catalogueLines = CatalogueReloadListener.current().linesFor(idOf(event));
        if (catalogueLines.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.villager_voices.debug.lines.empty", idOf(event)), false);
            return 0;
        }
        for (int i = 0; i < catalogueLines.size(); i++) {
            Line line = catalogueLines.get(i);
            int number = i + 1;
            source.sendSuccess(() -> Component.translatable(
                "command.villager_voices.debug.lines.line", number, line.text(), line.soundId()), false);
        }
        return catalogueLines.size();
    }

    /** Lists all 16 event ids this command (and the catalogue's per-event JSON files) accept. */
    static int events(CommandSourceStack source) {
        String joined = String.join(", ", EVENTS_BY_ID.keySet());
        source.sendSuccess(() -> Component.translatable(
            "command.villager_voices.debug.events.list", EVENTS_BY_ID.size(), joined), false);
        return EVENTS_BY_ID.size();
    }

    private static VillagerReactionEvent eventOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, "event");
        VillagerReactionEvent event = EVENTS_BY_ID.get(raw);
        if (event == null) {
            throw UNKNOWN_EVENT.create(raw, String.join(", ", EVENTS_BY_ID.keySet()));
        }
        return event;
    }

    private static Villager villagerOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "villager");
        if (!(entity instanceof Villager villager)) {
            throw NOT_A_VILLAGER.create();
        }
        return villager;
    }

    private static CompletableFuture<Suggestions> suggestEvents(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(EVENTS_BY_ID.keySet(), builder);
    }

    /**
     * Which silence rule (REACTION-REQ-009, REACTION-REQ-010) suppressed {@code event}, given
     * {@link ReactionRules#isSilenced} already returned {@code true} for these same arguments --
     * "asleep", "baby", or "asleep/baby" when a sleeping baby's own exempt events disagree (a
     * sleeping baby's {@code SLEEP} event, for instance, is still silenced by the baby rule alone).
     */
    private static String silenceReason(VillagerReactionEvent event, boolean asleep, boolean baby) {
        boolean byAsleep = asleep && event != VillagerReactionEvent.SLEEP;
        boolean byBaby = baby && event != VillagerReactionEvent.BABY_GROWS;
        if (byAsleep && byBaby) {
            return "asleep/baby";
        }
        return byAsleep ? "asleep" : "baby";
    }

    /** {@code event}'s catalogue key: its enum constant name, lower-cased. */
    private static String idOf(VillagerReactionEvent event) {
        return event.name().toLowerCase(Locale.ROOT);
    }

    private static Map<String, VillagerReactionEvent> buildEventsById() {
        Map<String, VillagerReactionEvent> byId = new LinkedHashMap<>();
        for (VillagerReactionEvent event : VillagerReactionEvent.values()) {
            byId.put(idOf(event), event);
        }
        return byId;
    }

    /**
     * Adapts the currently-loaded {@link Catalogue} (keyed by event id string, VV-3) to VV-2's
     * {@link LineCatalogue} (keyed by {@link VillagerReactionEvent}), so {@code trigger} can run the
     * real {@link VillagerEventBus} pipeline against real catalogue data. {@link LineRef#id()} is
     * set to each {@link Line}'s sound id, matching {@code LineRef}'s own Javadoc ("VV-3 proposes
     * the sound id") and {@link CapturingSink}'s own lookup back to the full {@link Line}.
     */
    private static final class CatalogueAdapter implements LineCatalogue {
        private final Catalogue catalogue;

        CatalogueAdapter(Catalogue catalogue) {
            this.catalogue = catalogue;
        }

        @Override
        public List<LineRef> linesFor(VillagerReactionEvent event) {
            return catalogue.linesFor(idOf(event)).stream()
                .map(line -> new LineRef(event, line.soundId()))
                .toList();
        }
    }

    /**
     * VV-2's {@link LineSink}, for this command's own purposes: resolves the selected
     * {@link LineRef} back to its full {@link Line} (subtitle text and sound id) for {@code
     * trigger}'s feedback message, and enqueues a real {@link DisplayLine} onto the mod's real
     * {@link DisplayQueue} for every recipient, exactly as the eventual production sink is expected
     * to (VV-8) -- so a forced event is visible on a nearby player's action bar, not just in the
     * command's own feedback.
     */
    private static final class CapturingSink implements LineSink {
        private final Catalogue catalogue;
        private final DisplayQueue queue;
        private final String speakerLabel;
        private Line captured;

        CapturingSink(Catalogue catalogue, DisplayQueue queue, String speakerLabel) {
            this.catalogue = catalogue;
            this.queue = queue;
            this.speakerLabel = speakerLabel;
        }

        @Override
        public void show(UUID villagerId, Set<UUID> playerIds, LineRef line) {
            Line resolved = catalogue.linesFor(idOf(line.event())).stream()
                .filter(candidate -> candidate.soundId().equals(line.id()))
                .findFirst()
                .orElse(null);
            captured = resolved;
            if (resolved == null) {
                return;
            }
            DisplayLine displayLine = new DisplayLine(speakerLabel, resolved.text(), resolved.soundId());
            for (UUID playerId : playerIds) {
                queue.enqueue(playerId, displayLine);
            }
        }
    }
}
