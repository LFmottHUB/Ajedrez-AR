package com.example.archess;
import java.nio.*;
import java.util.*;
import android.opengl.GLES20;

/** Lightweight procedural chessmen; no desktop model dependencies. */
final class Mesh {
    final FloatBuffer vertices;final int count;
    Mesh(List<Float> v){count=v.size()/6;vertices=ByteBuffer.allocateDirect(v.size()*4).order(ByteOrder.nativeOrder()).asFloatBuffer();for(float f:v)vertices.put(f);vertices.position(0);}
    static void tri(List<Float> a,float[] p,float[] q,float[] r){float ux=q[0]-p[0],uy=q[1]-p[1],uz=q[2]-p[2],vx=r[0]-p[0],vy=r[1]-p[1],vz=r[2]-p[2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len==0)len=1;for(float[] t:new float[][]{p,q,r}){for(float f:t)a.add(f);a.add(nx/len);a.add(ny/len);a.add(nz/len);}}
    static void box(List<Float> a,float x,float y,float z,float w,float d,float h){float[][] v={{x-w/2,y-d/2,z},{x+w/2,y-d/2,z},{x+w/2,y+d/2,z},{x-w/2,y+d/2,z},{x-w/2,y-d/2,z+h},{x+w/2,y-d/2,z+h},{x+w/2,y+d/2,z+h},{x-w/2,y+d/2,z+h}};for(int[] f:new int[][]{{0,3,2,1},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}}){tri(a,v[f[0]],v[f[1]],v[f[2]]);tri(a,v[f[0]],v[f[2]],v[f[3]]);}}
    static Mesh cube(){List<Float> a=new ArrayList<>();box(a,0,0,0,1,1,1);return new Mesh(a);}
    static void lathe(List<Float>a,float[][] profile){int n=24;for(int j=0;j<profile.length-1;j++)for(int i=0;i<n;i++){double t=i*2*Math.PI/n,u=(i+1)*2*Math.PI/n;float r=profile[j][0],z=profile[j][1],s=profile[j+1][0],h=profile[j+1][1];float[] p={(float)Math.cos(t)*r,(float)Math.sin(t)*r,z},q={(float)Math.cos(u)*r,(float)Math.sin(u)*r,z},v={(float)Math.cos(t)*s,(float)Math.sin(t)*s,h},w={(float)Math.cos(u)*s,(float)Math.sin(u)*s,h};tri(a,p,q,w);tri(a,p,w,v);}}
    static Mesh piece(char p){List<Float>a=new ArrayList<>();float[][] profile;
        switch(p){
            case 'p':profile=new float[][]{{0,0},{6,0},{6,2},{4,3},{2.6f,8},{4,9},{4.5f,11},{3.8f,14},{0,15}};break;
            case 'r':profile=new float[][]{{0,0},{7,0},{7,2},{5,4},{4,13},{6,15},{6,17},{0,17}};break;
            case 'b':profile=new float[][]{{0,0},{6.5f,0},{6.5f,2},{4,4},{2.7f,12},{5,13},{4,16},{2,20},{0,22}};break;
            case 'n':profile=new float[][]{{0,0},{6.5f,0},{6.5f,2},{4,4},{3.5f,9},{0,9}};break;
            case 'q':profile=new float[][]{{0,0},{7,0},{7,2},{5,4},{3,15},{5.5f,17},{6,21},{0,21}};break;
            default:profile=new float[][]{{0,0},{7,0},{7,2},{5,4},{3,16},{5,18},{4,21},{0,22}};
        }lathe(a,profile);
        if(p=='r')for(int i=0;i<4;i++){double t=i*Math.PI/2;box(a,(float)Math.cos(t)*4,(float)Math.sin(t)*4,17,3,3,4);}
        if(p=='k'){box(a,0,0,21,2.5f,2.5f,8);box(a,0,0,25,8,2.5f,2.5f);}
        if(p=='q')for(int i=0;i<6;i++){double t=i*Math.PI/3;box(a,(float)Math.cos(t)*4.5f,(float)Math.sin(t)*4.5f,21,2,2,3);}
        if(p=='n'){box(a,0,0,8,5,6,10);box(a,0,3,15,5,10,4);box(a,-1.5f,-1,18,1.5f,2,4);box(a,1.5f,-1,18,1.5f,2,4);}
        return new Mesh(a);
    }
    void draw(int position,int normal){vertices.position(0);GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,24,vertices);vertices.position(3);GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,24,vertices);GLES20.glEnableVertexAttribArray(position);GLES20.glEnableVertexAttribArray(normal);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);}
}
