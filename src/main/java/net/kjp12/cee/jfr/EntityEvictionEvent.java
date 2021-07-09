/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package net.kjp12.cee.jfr;// Created 2021-08-07T20:18:46

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLike;

/**
 * @author KJP12
 * @since 0.0.0
 **/
@Label("Entity Eviction")
@Description("Entities evicted during loading")
public class EntityEvictionEvent extends Event {
	@Label("Type")
	Class<? extends EntityLike> type;
	@Label("UUID")
	String uuid;
	@Label("Position X")
	double x;
	@Label("Position Y")
	double y;
	@Label("Position Z")
	double z;

	public EntityEvictionEvent() {
	}

	public EntityEvictionEvent(Entity entity) {
		this.type = entity.getType().getBaseClass();
		this.uuid = entity.getUuidAsString();
		this.x = entity.getX();
		this.y = entity.getY();
		this.z = entity.getZ();
	}
}
