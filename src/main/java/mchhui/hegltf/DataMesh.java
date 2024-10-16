package mchhui.hegltf;

import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class DataMesh {
    public String material;
    public boolean skin;

    protected List<Float> geoList = new ArrayList<>();
    protected int geoCount;
    protected ByteBuffer geoBuffer;
    protected IntBuffer elementBuffer;
    protected int elementCount;
    public int unit;
    public int glDrawingMode = GL11.GL_TRIANGLES;
    private int displayList = -1;
    private int ssboVao = -1;
    private int vertexCount = 0;
    private boolean compiled = false;
    private boolean compiling = false;
    private boolean initSkinning = false;

    // BUFFER OBJECT
    private int pos_vbo = -1;
    private int tex_vbo = -1;
    private int normal_vbo = -1;
    private int vbo = -1;
    private int ebo = -1;
    private int ssbo = -1;

    public void render() {
         if (!this.compiled) {
            try {
                compileVAO(1);
                return;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        // 如果需要 可加入纹理处理内容
        this.callVAO();
        
        if(ObjModelRenderer.glowTxtureMode) {
            if(!ObjModelRenderer.customItemRenderer.bindTextureGlow(ObjModelRenderer.glowType, ObjModelRenderer.glowPath)) {
                return;
            }
            float x = OpenGlHelper.lastBrightnessX;
            float y = OpenGlHelper.lastBrightnessY;
            ObjModelRenderer.glowTxtureMode=false;
            GlStateManager.depthMask(false);
            //GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            callVAO();
            GlStateManager.enableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            //GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            ObjModelRenderer.glowTxtureMode=true;
            ObjModelRenderer.customItemRenderer.bindTexture(ObjModelRenderer.glowType, ObjModelRenderer.glowPath);
            
            //垃圾bug 迟早把这改装界面扬了
            if(Minecraft.getMinecraft().currentScreen instanceof GuiGunModify) {
                GlStateManager.disableLighting();
            }
        }
    }

    private void compileVAO(float scale) {
        if (this.compiling) {
            return;
        }

        this.compiling = true;
        this.ssboVao = GL30.glGenVertexArrays();
        this.displayList = GL30.glGenVertexArrays();

        if (this.unit == 3) {
            final List<Float> geoList = this.geoList;
            this.vertexCount = geoList.size() / this.unit;

            FloatBuffer pos_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
            FloatBuffer tex_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
            FloatBuffer normal_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);

//            IntBuffer joint_intBuffer = BufferUtils.createIntBuffer(vertexCount * 4);
//            FloatBuffer weight_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 4);

            for (int i = 0, size = geoList.size(); i + 8 <= size; i += 8) {
                pos_floatBuffer.put(geoList.get(i));
                pos_floatBuffer.put(geoList.get(i + 1));
                pos_floatBuffer.put(geoList.get(i + 2));
                tex_floatBuffer.put(geoList.get(i + 3));
                tex_floatBuffer.put(geoList.get(i + 4));
                normal_floatBuffer.put(geoList.get(i + 5));
                normal_floatBuffer.put(geoList.get(i + 6));
                normal_floatBuffer.put(geoList.get(i + 7));
            }
            pos_floatBuffer.flip();
            tex_floatBuffer.flip();
            normal_floatBuffer.flip();

            GL30.glBindVertexArray(this.displayList);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            this.pos_vbo = GL15.glGenBuffers();
            this.tex_vbo = GL15.glGenBuffers();
            this.normal_vbo = GL15.glGenBuffers();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pos_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, pos_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tex_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tex_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normal_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normal_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            this.compiled = true;
            this.compiling = false;

        } else {
            this.vbo = GL15.glGenBuffers();
            this.ebo = GL15.glGenBuffers();
            this.geoBuffer.flip();
            this.elementBuffer.flip();
            GL30.glBindVertexArray(this.displayList);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.geoBuffer, GL15.GL_STATIC_DRAW);
            int step = 17 * Float.BYTES;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, step, 0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, step, 3 * Float.BYTES);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, step, 5 * Float.BYTES);
            // in fact, it is u_int:
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, step, 8 * Float.BYTES);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, step, 12 * Float.BYTES);
            // in fact, it is u_int:
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, step, 16 * Float.BYTES);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer, GL15.GL_STATIC_DRAW);
            this.ssbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, this.geoBuffer, GL15.GL_DYNAMIC_COPY);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            
            GL30.glBindVertexArray(this.ssboVao);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.ssbo);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 8 * Float.BYTES, 0);
            GL11.glNormalPointer(GL11.GL_FLOAT, 8 * Float.BYTES, 3 * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 8 * Float.BYTES, 6 * Float.BYTES);
            
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);

            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

            this.skin = true;
            this.compiled = true;
            this.compiling = false;
        }

        //内存优化
        if(this.geoList != null) {
            this.geoList.clear();
            this.geoList = null;
        }
        if(this.geoBuffer != null) {
            if(((sun.nio.ch.DirectBuffer)this.geoBuffer).cleaner() != null) {
                ((sun.nio.ch.DirectBuffer)this.geoBuffer).cleaner().clean();
            }
        }
        if(this.elementBuffer!=null) {
            if(((sun.nio.ch.DirectBuffer)this.elementBuffer).cleaner() != null) {
                ((sun.nio.ch.DirectBuffer)this.elementBuffer).cleaner().clean();
            }
        }
    }

    public void callSkinning() {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, this.ssbo);
            GL30.glBindVertexArray(this.displayList);
            GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
            this.initSkinning = true;
        }
    }

    private void callVAO() {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            if (!this.initSkinning) {
                return;
            }
            GL30.glBindVertexArray(this.ssboVao);
            GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            GL30.glBindVertexArray(this.displayList);
            GL11.glDrawArrays(this.glDrawingMode, 0, this.vertexCount);
            GL30.glBindVertexArray(0);
        }
    }

    public void delete() {
        // It will be auto clean.
        GL30.glDeleteVertexArrays(this.displayList);
        GL30.glDeleteVertexArrays(this.ssboVao);
        if (this.pos_vbo != -1) {
            GL15.glDeleteBuffers(this.pos_vbo);
        }
        if (this.tex_vbo != -1) {
            GL15.glDeleteBuffers(this.tex_vbo);
        }
        if (this.normal_vbo != -1) {
            GL15.glDeleteBuffers(this.normal_vbo);
        }
        if (this.vbo != -1) {
            GL15.glDeleteBuffers(this.vbo);
        }
        if (this.ebo != -1) {
            GL15.glDeleteBuffers(this.ebo);
        }
        if (this.ssbo != -1) {
            GL15.glDeleteBuffers(this.ssbo);
        }
    }
}
