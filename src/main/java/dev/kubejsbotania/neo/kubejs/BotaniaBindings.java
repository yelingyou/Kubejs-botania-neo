package dev.kubejsbotania.neo.kubejs;

import dev.latvian.mods.kubejs.script.BindingRegistry;
import vazkii.botania.api.corporea.CorporeaHelper;
import vazkii.botania.api.mana.ManaItemHandler;

/**
 * Registers the global script bindings that KubeJS-Botania 1.20.1 exposed.
 *
 * <p>Both Botania 1.21.1 singletons still exist with the same accessor style as in 1.20.1:</p>
 * <ul>
 *     <li>{@code ManaHandler} - {@link ManaItemHandler#instance()},
 *     {@code vazkii.botania.api.mana.ManaItemHandler}</li>
 *     <li>{@code CorporeaHelper} - {@link CorporeaHelper#instance()},
 *     {@code vazkii.botania.api.corporea.CorporeaHelper}</li>
 * </ul>
 *
 * <p>Unlike the old KubeJS-Botania plugin there is no manual {@code RegistryInfo<Brew>} registration
 * anymore: KubeJS 2101 discovers custom registries through the platform's registry system instead
 * (Botania's {@code botania:brews} registry is a normal NeoForge registry). The brew recipe schema
 * reaches it through Botania's own {@code byNameCodec()}.</p>
 *
 * <p>Script bindings are global, so the bean-style names kept from 1.20.1 are intentional.</p>
 */
public final class BotaniaBindings {
	/**
	 * Name of the {@link ManaItemHandler} script binding.
	 */
	public static final String MANA_HANDLER = "ManaHandler";

	/**
	 * Name of the {@link CorporeaHelper} script binding.
	 */
	public static final String CORPOREA_HELPER = "CorporeaHelper";

	private BotaniaBindings() {
	}

	/**
	 * Registers the Botania script bindings. Called from
	 * {@link dev.latvian.mods.kubejs.plugin.KubeJSPlugin#registerBindings}.
	 *
	 * @param bindings the KubeJS binding registry
	 */
	public static void register(BindingRegistry bindings) {
		bindings.add(MANA_HANDLER, ManaItemHandler.instance());
		bindings.add(CORPOREA_HELPER, CorporeaHelper.instance());
	}
}
