package villager_voices.fabric.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.AnimSetupContext;
import traben.entity_model_features.models.animation.math.expression_tree.MathValue;
import traben.entity_model_features.models.animation.math.variables.factories.UniqueVariableFactory;
import traben.entity_model_features.models.animation.state.EMFState;
import villager_voices.fabric.VillagerVoicesFabric;

/**
 * The soft-dependency half of VV-12 (docs/spec/domains/compat.md {@code COMPAT-REQ-004}): exposes
 * {@code villager_voices.is_talking} to EMF via
 * {@code EMFAnimationApi.registerUniqueAnimationVariableFactory(String, String,
 * UniqueVariableFactory)} — reading it back from the exact same {@link TalkingRenderState#IS_TALKING}
 * render-state entry {@link VillagerTalkingRenderStateMixin}/
 * {@link ZombieVillagerTalkingRenderStateMixin} already copied it into
 * ({@code COMPAT-DEC-001}: one copy, one reader, never a second parallel lookup), via
 * {@code EMFState.state()} — EMF's own duck-interface over the very same
 * {@code EntityRenderState} instance Fabric API's {@link FabricRenderState} is mixed into, both
 * present on one object during the animation pass.
 *
 * <p><b>The API surface actually shipped differs from the research note's approximation</b> (this
 * ticket's own Findings): {@code registerUniqueAnimationVariableFactory} takes {@code (String
 * sourceModId, String variableName, UniqueVariableFactory factory)} — three arguments, not four; the
 * {@code variableName} argument is used only for EMF's own log line (confirmed by decompiling the
 * shipped {@code 3.3.8-fabric-26.2} jar's bytecode), <em>not</em> the string an animation's
 * {@code query.variable(...)} call names — matching is entirely
 * {@link UniqueVariableFactory#createsThisVariable(String)}'s own job, implemented below against
 * {@link #VARIABLE_ID} (the fully-dotted {@code villager_voices.is_talking}, the reference snippet's
 * own {@code query.variable('villager_voices.is_talking')}).
 *
 * <p><b>Never referenced except behind {@code FabricLoader.isModLoaded("entity_model_features")}</b>
 * ({@link villager_voices.fabric.client.VillagerVoicesFabricClient#onInitializeClient()}) — every
 * import above resolves against EMF's (and, transitively, ETF's — EMF's own {@code fabric.mod.json}
 * hard-depends on {@code entity_texture_features}, and {@code EMFState}'s own return type extends an
 * ETF interface, confirmed to matter at <em>compile</em> time too, this ticket's own Findings) API
 * jars, both {@code modCompileOnly} in {@code fabric/build.gradle.kts}, present on neither the
 * runtime classpath nor the shipped jar when EMF is absent ({@code COMPAT-FAIL-001}) — the JVM never
 * loads this class, and so never resolves those imports, unless that guard already passed.
 */
@Environment(EnvType.CLIENT)
public final class EmfCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoicesFabric.MOD_ID);
    private static final String VARIABLE_NAME = "is_talking";

    /** The fully-dotted variable id a compatible animation's {@code query.variable(...)} names. */
    static final String VARIABLE_ID = VillagerVoicesFabric.MOD_ID + "." + VARIABLE_NAME;

    private EmfCompat() {
    }

    /** Registers {@link #VARIABLE_ID} against EMF. Call only once EMF is confirmed loaded. */
    public static void register() {
        try {
            EMFAnimationApi.registerUniqueAnimationVariableFactory(
                    VillagerVoicesFabric.MOD_ID, VARIABLE_NAME, new IsTalkingVariableFactory());
            LOGGER.info("villager_voices: registered {} for EMF", VARIABLE_ID);
        } catch (Exception e) {
            LOGGER.warn("villager_voices: failed to register the EMF {} variable "
                    + "(EMF mouth-movement cues will not work this session)", VARIABLE_ID, e);
        }
    }

    private static float currentValue() {
        if (EMFState.state() instanceof FabricRenderState renderState) {
            return renderState.getDataOrDefault(TalkingRenderState.IS_TALKING, Boolean.FALSE) ? 1f : 0f;
        }
        return 0f; // no current render state (e.g. called outside an animation pass) -- not talking.
    }

    private static final class IsTalkingVariableFactory extends UniqueVariableFactory {

        @Override
        public MathValue.ResultSupplier getSupplierOrNull(String variableString, AnimSetupContext context) {
            return createsThisVariable(variableString) ? EmfCompat::currentValue : null;
        }

        @Override
        public boolean createsThisVariable(String variableString) {
            return VARIABLE_ID.equals(variableString);
        }

        @Override
        public String getExplanationTranslationKey() {
            return "villager_voices.emf.is_talking.explanation";
        }

        @Override
        public String getTitleTranslationKey() {
            return "villager_voices.emf.is_talking.title";
        }
    }
}
