/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package net.kjp12.cee;// Created 2021-08-07T20:17:49

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * A fusebox of options to enable and disable features in code.
 *
 * No external configuration is available at this time.
 *
 * @implNote There <em>must not</em> be any calls to Minecraft or other mods.
 * @author KJP12
 * @since 0.0.0
 **/
public final class Fusebox {
	private Fusebox() {
	}

	public static final boolean JFR = true;
	public static final boolean JDBC = true;
	public static final String JDBC_PROTOCOL_BASE = "jdbc:h2:";
	public static final boolean SERVER = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	public static final boolean LEDGER = SERVER && FabricLoader.getInstance().isModLoaded("ledger");
	public static final int CHUNK_LIMIT = 256;
}
