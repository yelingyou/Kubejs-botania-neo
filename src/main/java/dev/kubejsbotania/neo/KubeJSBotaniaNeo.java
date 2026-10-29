package dev.kubejsbotania.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KubeJS Botania Neo 主类 / main mod class.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Register the extended Terra Plate recipe serializer (KubeJS writes the recipes).</li>
 *     <li>Everything KubeJS related happens through {@code kubejs.plugins.txt} and
 *     {@link dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin}.</li>
 *     <li>Terra Plate behaviour is added by {@code TerrestrialAgglomerationPlateBlockEntityMixin}.</li>
 * </ul>
 */
@Mod(KubeJSBotaniaNeo.MOD_ID)
public class KubeJSBotaniaNeo {
	public static final String MOD_ID = "kubejs_botania_neo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public KubeJSBotaniaNeo(IEventBus modEventBus, ModContainer modContainer) {
		ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
		LOGGER.info("[{}] initialized", MOD_ID);
	}
}
