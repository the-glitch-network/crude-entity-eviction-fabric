/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package net.kjp12.cee;// Created 2021-08-07T18:40:06

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author KJP12
 * @since 0.0.0
 **/
public class Main {
	public static final Logger logger = LogManager.getLogger("Crude Entity Eviction");
	public static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private static JdbcDriver driver;

	public static final DamageSource ANGRY_LANDLORD = new DamageSource("cee.angryLandlord") {
	};

	public static void init() {
		if (Fusebox.JDBC)
			try {
				driver = new JdbcDriver();
				ServerWorldEvents.LOAD.register(driver);
				ServerWorldEvents.UNLOAD.register(driver);
			} catch (SQLException sql) {
				logger.error("Failed to load JDBC driver.", sql);
			}
	}

	public static void submitEvictionNotice(Entity entity) {
		if (driver != null) {
			driver.queue(entity);
		}
	}
}
