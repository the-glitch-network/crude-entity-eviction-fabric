/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package net.kjp12.cee.mixin;// Created 2021-08-07T19:04:52

import com.github.quiltservertools.ledger.actionutils.ActionFactory;
import jdk.jfr.Frequency;
import jdk.jfr.Label;
import net.kjp12.cee.Fusebox;
import net.kjp12.cee.Main;
import net.kjp12.cee.jfr.EntityEvictionEvent;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author KJP12
 * @since 0.0.0
 **/
@Mixin(ServerEntityManager.class)
public class MixinServerEntityManager<T extends EntityLike> {
	@Inject(method = "addEntity(Lnet/minecraft/world/entity/EntityLike;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityTrackingSection;add(Ljava/lang/Object;)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
	@Label("Entity eviction check")
	@Frequency
	private void cee$onAddEntity(T entityLike, boolean existing, CallbackInfoReturnable<Boolean> cir, long chunkPos,
			EntityTrackingSection<T> section) {
		if (!(entityLike instanceof Entity entity))
			return;
		if (entity.hasCustomName() || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity
				|| entity instanceof ProjectileEntity || entity instanceof MinecartEntity || entity instanceof Inventory
				|| entity instanceof PlayerEntity || entity instanceof Bucketable
				|| entity instanceof ElderGuardianEntity || entity instanceof WitherEntity
				|| entity instanceof EnderDragonEntity || entity instanceof AreaEffectCloudEntity
				|| entity instanceof ArmorStandEntity || entity instanceof AbstractDecorationEntity
				|| entity instanceof BeeEntity || entity instanceof FallingBlockEntity
				|| entity instanceof EndCrystalEntity || entity instanceof GolemEntity
				|| entity instanceof FlyingItemEntity || entity instanceof EvokerFangsEntity
				|| entity instanceof TntEntity || entity instanceof VexEntity || entity instanceof VillagerEntity) {
			return; // don't evict items, xp, projectiles, fish, axolotls or players
		}
		if (entity instanceof MobEntity mob) {
			if (mob.isPersistent() || mob.cannotDespawn())
				return; // don't evict persistent entities
		}
		if (entity instanceof Tameable tameable) {
			if (tameable.getOwnerUuid() != null)
				return; // don't eat pets
		}
		if (entity instanceof Saddleable saddleable) {
			if (saddleable.isSaddled())
				return; // once more
		}
		if (entity instanceof HorseBaseEntity horse) {
			if (horse.isTame())
				return; // don't eat the pets
			if (entity instanceof DonkeyEntity donkey) {
				if (donkey.hasChest())
					return;
			}
		}
		if (section.size() > Fusebox.CHUNK_LIMIT) {
			Main.logger.info("Evicted {}", entity);
			Main.submitEvictionNotice(entity);
			entity.remove(Entity.RemovalReason.DISCARDED);

			if (Fusebox.LEDGER && entity instanceof LivingEntity living) {
				ActionFactory.INSTANCE.entityKillAction(entity.world, entity.getBlockPos(), living,
						Main.ANGRY_LANDLORD);
			}
			if (Fusebox.JFR) {
				new EntityEvictionEvent(entity).commit();
			}
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
