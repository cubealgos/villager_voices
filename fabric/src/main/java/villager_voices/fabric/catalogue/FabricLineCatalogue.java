package villager_voices.fabric.catalogue;

import villager_voices.LineCatalogue;
import villager_voices.LineRef;
import villager_voices.VillagerReactionEvent;
import villager_voices.catalogue.Catalogue;
import villager_voices.catalogue.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapts VV-3's {@link Catalogue} (keyed by event id string, e.g. {@code "trade_completed"}) to
 * VV-2's {@link LineCatalogue} (keyed by {@link VillagerReactionEvent}) — the small piece of glue
 * both packages' own Javadoc anticipated (VV-2's {@code LineCatalogue}: "VV-3... implements it
 * after this ticket merges"; VV-3's {@code Catalogue}: "the merge maps a {@code ReactionEvent}
 * constant to this key"). Stateless: always reads through to
 * {@link CatalogueReloadListener#current()}, so a datapack reload is reflected on the very next
 * call, with nothing here to invalidate.
 */
public final class FabricLineCatalogue implements LineCatalogue {

    @Override
    public List<LineRef> linesFor(VillagerReactionEvent event) {
        String eventId = event.name().toLowerCase(Locale.ROOT);
        List<Line> lines = CatalogueReloadListener.current().linesFor(eventId);
        List<LineRef> refs = new ArrayList<>(lines.size());
        for (Line line : lines) {
            refs.add(new LineRef(event, line.soundId()));
        }
        return List.copyOf(refs);
    }
}
