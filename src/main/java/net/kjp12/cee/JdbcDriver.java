/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package net.kjp12.cee;// Created 2021-09-07T00:31:48

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.kjp12.cee.mixin.AccessorMinecraftServer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Contract;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * A plain abstract JDBC driver to allow for storing away evicted entities.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class JdbcDriver implements ServerWorldEvents.Load, ServerWorldEvents.Unload {
	private final ReferenceQueue<? super ServerWorld> queue = new ReferenceQueue<>();
	private final Driver driver;
	private static final VarHandle handle = MethodHandles.arrayElementVarHandle(JdbcHandler[].class);
	private JdbcHandler[] handlers = new JdbcHandler[32];

	public JdbcDriver() throws SQLException {
		this.driver = DriverManager.getDriver(Fusebox.JDBC_PROTOCOL_BASE + ":memory");
		Main.service.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void onWorldLoad(MinecraftServer server, ServerWorld world) {
		try {
			getHandler(world, false).spawn();
		} catch (SQLException sql) {
			Main.logger.error("Failed to spawn eviction queue.", sql);
		}
	}

	@Override
	public void onWorldUnload(MinecraftServer server, ServerWorld world) {
		removeHandler(getHandler(world, true));
	}

	public void queue(Entity entity) {
		getHandler((ServerWorld) entity.world, false).queue(entity);
	}

	public void tick() {
		var list = new ArrayList<Throwable>();
		{
			JdbcHandler handler;
			while ((handler = (JdbcHandler) queue.poll()) != null) {
				removeHandler(handler);
			}
		}
		for (var handler : handlers) {
			if (handler == null)
				continue;
			try {
				handler.tick();
			} catch (SQLException sql) {
				for (var t : sql)
					list.add(t);
			} catch (Exception e) {
				list.add(e);
			}
		}
		if (!list.isEmpty()) {
			var e = new Throwable();
			for (var t : list)
				e.addSuppressed(t);
			Main.logger.error("Exceptions were thrown during execution", e);
		}
	}

	private synchronized void removeHandler(JdbcHandler handler) {
		if (handler == null)
			return;
		try {
			handler.close();
		} catch (Exception sql) {
			Main.logger.warn("Failed to close closing handler {}", handler, sql);
		}
		handle.compareAndSet(handlers, handler.index, handler, null);
	}

	@Contract("_, true -> _;_, false -> !null")
	private synchronized JdbcHandler getHandler(ServerWorld world, boolean isRemoval) {
		do {
			int hash = handlers.length - 1;
			int index = world.hashCode() & hash;
			var handler = handlers[index];
			if (handler != null) {
				if (handler.refersTo(world))
					return handler;
				rehash();
			} else if (isRemoval) {
				return null;
			} else {
				return handlers[index] = new JdbcHandler(world, index);
			}
		} while (true);
	}

	private void rehash() {
		var old = handlers;
		handlers = new JdbcHandler[old.length << 1];
		int hash = handlers.length - 1;
		for (var handler : old) {
			if (handler == null)
				continue;
			var world = handler.get();
			if (world == null) {
				handler.enqueue();
			} else {
				handlers[handler.index = world.hashCode() & hash] = handler;
			}
		}
	}

	private class JdbcHandler extends WeakReference<ServerWorld> {
		private final Queue<Entity> queue = new ConcurrentLinkedQueue<>();
		private int index;
		private Connection connection;
		private PreparedStatement statement;

		private JdbcHandler(ServerWorld world, int index) {
			super(world, JdbcDriver.this.queue);
			this.index = index;
		}

		private void queue(Entity entity) {
			this.queue.add(entity);
		}

		private synchronized void tick() throws Exception {
			if (queue.isEmpty())
				return;
			if (connection == null || statement == null || connection.isClosed() || statement.isClosed())
				this.spawn();
			var list = new ArrayList<Throwable>();
			Entity entity;
			while ((entity = queue.poll()) != null)
				try {
					var pos = entity.getPos();
					statement.setTimestamp(1, Timestamp.from(Instant.now()));
					statement.setString(2, Registry.ENTITY_TYPE.getId(entity.getType()).toString());
					statement.setDouble(3, pos.x);
					statement.setDouble(4, pos.y);
					statement.setDouble(5, pos.z);
					ByteArrayInputStream bis = null;
					try (var bos = new ByteArrayOutputStream(); var dos = new DataOutputStream(bos)) {
						NbtIo.write(entity.writeNbt(new NbtCompound()), dos);
						bis = new ByteArrayInputStream(bos.toByteArray());
					} catch (IOException ioe) {
						// non-fatal, log and continue
						list.add(ioe);
					}
					statement.setBlob(6, bis);
					statement.addBatch();
				} catch (SQLException sql) {
					for (var t : sql)
						list.add(t);
				}
			try {
				statement.executeBatch();
			} catch (SQLException sql) {
				for (var t : sql)
					list.add(t);
			}
			if (!list.isEmpty()) {
				var e = new Exception("Failed to send batches.");
				for (var t : list)
					e.addSuppressed(t);
				throw e;
			}
		}

		private synchronized void close() throws Exception {
			var list = new ArrayList<Throwable>();
			try {
				this.tick();
			} catch (SQLException sql) {
				for (var t : sql)
					list.add(t);
			} catch (Exception e) {
				list.add(e);
			}
			this.enqueue();
			if (this.statement != null)
				try {
					this.statement.close();
				} catch (SQLException sql) {
					for (var t : sql)
						list.add(t);
				} catch (Exception e) {
					list.add(e);
				}
			if (this.connection != null)
				try {
					this.connection.close();
				} catch (SQLException sql) {
					for (var t : sql)
						list.add(t);
				}
			if (!list.isEmpty()) {
				var e = new Exception("Failed to close");
				for (var t : list)
					e.addSuppressed(t);
				throw e;
			}
		}

		private void spawn() throws SQLException {
			var world = get();
			if (world == null) {
				// We cannot really do much with nothing.
				return;
			}
			if (this.connection == null || this.connection.isClosed()) {
				var worldDirectory = ((AccessorMinecraftServer) world.getServer()).getSession()
						.getWorldDirectory(world.getRegistryKey());
				var database = new File(worldDirectory, "cee-evicted-entities.db");
				this.connection = driver.connect(Fusebox.JDBC_PROTOCOL_BASE + database.toURI(), new Properties());
				this.connection.prepareStatement(
						"create table if not exists evicted(time timestamp,type varchar,x double,y double,z double,nbt blob)")
						.execute();
			}
			if (this.statement == null || this.statement.isClosed()) {
				this.statement = connection
						.prepareStatement("insert into evicted(time,type,x,y,z,nbt)values(?,?,?,?,?,?)");
			}
		}
	}
}
