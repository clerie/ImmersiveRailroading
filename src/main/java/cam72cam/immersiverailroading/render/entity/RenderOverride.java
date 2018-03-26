package cam72cam.immersiverailroading.render.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

public class RenderOverride {
	
	private static Vec3d getCameraPos(float partialTicks) {
        Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
	}
	
	private static ICamera getCamera(float partialTicks) {
        ICamera camera = new Frustum();
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.xCoord, cameraPos.yCoord, cameraPos.zCoord);
        return camera;
	}
	
	public static void renderStock(float partialTicks) {
        int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 0 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
        
		Minecraft.getMinecraft().mcProfiler.startSection("ir_entity");

        ICamera camera = getCamera(partialTicks);
        
        World world = Minecraft.getMinecraft().thePlayer.getEntityWorld();
        List<EntityRollingStock> entities = world.getEntities(EntityRollingStock.class, EntitySelectors.IS_ALIVE);
        for (EntityRollingStock entity : entities) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        	}
        }

        Minecraft.getMinecraft().mcProfiler.endSection();;
	}
	
	public static void renderParticles(float partialTicks) {
		int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 1 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
		Minecraft.getMinecraft().mcProfiler.startSection("ir_particles");
		
		GlStateManager.depthMask(false);
		
        ICamera camera = getCamera(partialTicks);
        Vec3d ep = getCameraPos(partialTicks);
       
        World world = Minecraft.getMinecraft().thePlayer.getEntityWorld();
        List<EntitySmokeParticle> smokeEnts = world.getEntities(EntitySmokeParticle.class, EntitySelectors.IS_ALIVE);
        Comparator<EntitySmokeParticle> compare = (EntitySmokeParticle e1, EntitySmokeParticle e2) -> {
        	Double p1 = e1.getPositionVector().squareDistanceTo(ep);
        	Double p2 = e1.getPositionVector().squareDistanceTo(ep);
        	return p1.compareTo(p2);
        };
        Minecraft.getMinecraft().mcProfiler.startSection("ent_sort");
    	Collections.sort(smokeEnts,  compare);
        Minecraft.getMinecraft().mcProfiler.endSection();

        ParticleRender.shader.bind();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		Minecraft.getMinecraft().mcProfiler.startSection("render_particle");
        for (EntitySmokeParticle entity : smokeEnts) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        	}
        }
        Minecraft.getMinecraft().mcProfiler.endSection();

		blend.restore();
		tex.restore();
		cull.restore();
		light.restore();
		
		ParticleRender.shader.unbind();
		

		GlStateManager.depthMask(true);
		
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	public static void renderTiles(float partialTicks) {
        int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 0 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
        
		Minecraft.getMinecraft().mcProfiler.startSection("ir_tile");

        ICamera camera = getCamera(partialTicks);
        Vec3d cameraPos = getCameraPos(partialTicks);
        
        GL11.glPushMatrix();
        {
	        GL11.glTranslated(-cameraPos.xCoord, -cameraPos.yCoord, -cameraPos.zCoord);
			GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, false);
		
	        OBJRender model = RailBuilderRender.getModel(Gauge.STANDARD); 
	        model.bindTexture();
	        List<TileEntity> entities = Minecraft.getMinecraft().thePlayer.getEntityWorld().loadedTileEntityList;
	        for (TileEntity te : entities) {
	        	if (te instanceof TileRail && camera.isBoundingBoxInFrustum(te.getRenderBoundingBox())) {
	        		Vec3d relPos = new Vec3d(te.getPos());
	        		
	        		RailInfo info = ((TileRail) te).getRailRenderInfo();
	        		if (info == null) {
	        			// Still loading...
	        			continue;
	        		}
	        		
	        		GL11.glPushMatrix();
	        		{
	        	        int i = te.getWorld().getCombinedLight(te.getPos(), 0);
	        	        int j = i % 65536;
	        	        int k = i / 65536;
	        	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);
	        			info = info.clone();
    					GL11.glTranslated(relPos.xCoord, relPos.yCoord, relPos.zCoord);	
	        			if (info.type == TrackItems.SWITCH) {
	        				info.type = TrackItems.STRAIGHT;
	        			}
		        		RailBuilderRender.renderRailBuilder(info);
	        		}
	        		GL11.glPopMatrix();
	        	}
	        }
	        model.restoreTexture();
	        
	        blend.restore();
        }
        GL11.glPopMatrix();
        Minecraft.getMinecraft().mcProfiler.endSection();;
	}
}
 