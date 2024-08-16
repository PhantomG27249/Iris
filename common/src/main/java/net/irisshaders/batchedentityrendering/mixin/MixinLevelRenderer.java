package net.irisshaders.batchedentityrendering.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/**
 * Tracks whether or not the world is being rendered, and manages grouping
 * with different entities.
 */
// Uses a priority of 999 to apply before the main Iris mixins to draw entities before deferred runs.
@Mixin(value = LevelRenderer.class, priority = 999)
public class MixinLevelRenderer {
	private static final String RENDER_ENTITY =
		"Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V";

	@Shadow
	private RenderBuffers renderBuffers;

	@Unique
	private Groupable groupable;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void batchedentityrendering$beginLevelRender(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
		if (renderBuffers instanceof DrawCallTrackingRenderBuffers) {
			((DrawCallTrackingRenderBuffers) renderBuffers).resetDrawCounts();
		}

		((RenderBuffersExt) renderBuffers).beginLevelRendering();
		MultiBufferSource provider = renderBuffers.bufferSource();

		if (provider instanceof Groupable) {
			groupable = (Groupable) provider;
		}
	}

	@Inject(method = "renderEntities", at = @At(value = "INVOKE", target = RENDER_ENTITY))
	private void batchedentityrendering$preRenderEntity(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci) {
		if (groupable != null) {
			groupable.startGroup();
		}
	}

	@Inject(method = "renderEntities", at = @At(value = "INVOKE", target = RENDER_ENTITY, shift = At.Shift.AFTER))
	private void batchedentityrendering$postRenderEntity(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci) {
		if (groupable != null) {
			groupable.endGroup();
		}
	}

	@Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=translucent"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void batchedentityrendering$beginTranslucents(FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, boolean bl, ResourceHandle resourceHandle5, CallbackInfo ci, float f, Vec3 vec3, double d, double e, double g, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		if (renderBuffers.bufferSource() instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource) {
			fullyBufferedMultiBufferSource.readyUp();
		}

		if (WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws()) {
			Minecraft.getInstance().getProfiler().popPush("entity_draws_opaque");
			if (renderBuffers.bufferSource() instanceof FullyBufferedMultiBufferSource source) {
				source.endBatchWithType(TransparencyType.OPAQUE);
				source.endBatchWithType(TransparencyType.OPAQUE_DECAL);
			} else {
				this.renderBuffers.bufferSource().endBatch();
			}
		} else {
			Minecraft.getInstance().getProfiler().popPush("entity_draws");
			this.renderBuffers.bufferSource().endBatch();
		}
	}


	@Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=translucent", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	private void batchedentityrendering$endTranslucents(FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, boolean bl, ResourceHandle resourceHandle5, CallbackInfo ci, float f, Vec3 vec3, double d, double e, double g, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		if (WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws()) {
			this.renderBuffers.bufferSource().endBatch();
		}
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void batchedentityrendering$endLevelRender(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
		((RenderBuffersExt) renderBuffers).endLevelRendering();
		groupable = null;
	}
}