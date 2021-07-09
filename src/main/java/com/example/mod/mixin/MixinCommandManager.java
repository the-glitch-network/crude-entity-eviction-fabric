package com.example.mod.mixin;

import com.example.mod.Example;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Small mixin of logging command executions.
 *
 * @since 0.0.0
 **/
@Mixin(CommandManager.class)
public abstract class MixinCommandManager {
	@Inject(method = "execute", at = @At("HEAD"))
	public void example$execute$logCommandExecution(ServerCommandSource source, String command,
			CallbackInfoReturnable<Integer> cir) {
		Example.logger.info("{} executed {}", source.getName(), command);
	}
}
