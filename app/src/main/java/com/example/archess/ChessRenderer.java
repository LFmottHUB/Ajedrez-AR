package com.example.archess;

import android.opengl.*;
import android.os.SystemClock;
import java.io.*;
import java.util.*;
import org.artoolkitx.arx.arxj.ARController;
import org.artoolkitx.arx.arxj.ARX_jni;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class ChessRenderer implements GLSurfaceView.Renderer {
    interface Listener {void tracking(String text,boolean visible);void squareTapped(int square);}
    private final Game game;private final String assetsDir;private final Listener listener;
    private ARController ar;private boolean initialized=false,video=false,failed=false;
    private int marker=-1,w=1,h=1,program,locMvp,locModel,locColor,locPos,locNorm;
    private final int[] vp=new int[4];private final Map<Character,Mesh> pieces=new HashMap<>();private Mesh cube;
    private final float[] pose=new float[16];private float[] projection;
    private volatile float tapX=-1,tapY=-1;volatile float boardScale=1.6f;volatile boolean showPieces=true;
    private long lastFrame=0,lastReport=0,startTime=0;private boolean visible=false;private int stableFrames=0;
    ChessRenderer(Game game,String dir,Listener l){this.game=game;assetsDir=dir;listener=l;}
    void tap(float x,float y){tapY=y;tapX=x;}
    private void report(String s,boolean v){listener.tracking(s,v);}
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        failed=false;video=false;visible=false;lastFrame=0;startTime=SystemClock.elapsedRealtime();
        try {
            ar=ARController.getInstance();if(!ar.initialiseNative(assetsDir))throw new IOException("No se pudo cargar artoolkitX. Revisa la compatibilidad del teléfono.");initialized=true;
            // Offline rear camera. The SDK estimates calibration when no device profile is available.
            if(!ARX_jni.arwStartRunning("-native -position=back -width=1280 -height=720 -prefer=closestpixelcount -cachedir="+assetsDir,null))throw new IOException("No se pudo abrir la cámara trasera.");
            ARX_jni.arwSetTrackerOptionInt(ARX_jni.ARW_TRACKER_OPTION_SQUARE_PATTERN_DETECTION_MODE,ARX_jni.AR_TEMPLATE_MATCHING_MONO);
            ARX_jni.arwSetTrackerOptionInt(ARX_jni.ARW_TRACKER_OPTION_SQUARE_THRESHOLD_MODE,ARX_jni.AR_LABELING_THRESH_MODE_AUTO_OTSU);
            ar.setBorderSize(0.25f);
            marker=ar.addTrackable("single;Data/selector.patt;100");if(marker<0)throw new IOException("No se pudo cargar el patrón selector.");
            ARX_jni.arwSetTrackableOptionBool(marker,ARX_jni.ARW_TRACKABLE_OPTION_FILTERED,true);
            ARX_jni.arwSetTrackableOptionFloat(marker,ARX_jni.ARW_TRACKABLE_OPTION_FILTER_SAMPLE_RATE,30);
            ARX_jni.arwSetTrackableOptionFloat(marker,ARX_jni.ARW_TRACKABLE_OPTION_FILTER_CUTOFF_FREQ,8);
            ARX_jni.arwSetTrackableOptionFloat(marker,ARX_jni.ARW_TRACKABLE_OPTION_SQUARE_CONFIDENCE_CUTOFF,0.60f);
            program=makeProgram();locPos=GLES20.glGetAttribLocation(program,"aPosition");locNorm=GLES20.glGetAttribLocation(program,"aNormal");locMvp=GLES20.glGetUniformLocation(program,"uMvp");locModel=GLES20.glGetUniformLocation(program,"uModel");locColor=GLES20.glGetUniformLocation(program,"uColor");
            cube=Mesh.cube();pieces.clear();for(char p:"pnbrqk".toCharArray())pieces.put(p,Mesh.piece(p));
            report("Cámara iniciando · enfoca un solo marcador",false);
        }catch(Throwable e){failed=true;report("Error AR: "+e.getMessage(),false);close();}
    }
    @Override public void onSurfaceChanged(GL10 gl,int width,int height){w=width;h=height;if(video)settings();}
    private void settings(){ar.drawVideoSettings(0,w,h,false,false,false,ARX_jni.ARW_H_ALIGN_CENTRE,ARX_jni.ARW_V_ALIGN_CENTRE,ARX_jni.ARW_SCALE_MODE_FIT,vp);}
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClearColor(.035f,.045f,.06f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        if(failed||!initialized)return;
        long now=SystemClock.elapsedRealtime();
        try {
            // capture/update must run while WAITING_FOR_VIDEO: update advances the native
            // controller to DETECTION_RUNNING. Gating them on isRunning deadlocks startup.
            if(ar.capture()){
                boolean updated=ar.update();lastFrame=now;
                boolean found=updated&&ar.queryTrackableVisibilityAndTransformation(marker,pose);
                stableFrames=found?stableFrames+1:0;visible=found&&stableFrames>=2;
            }
            if(!ar.isRunning()){if(now-startTime>8000&&now-lastReport>2000){report("La cámara no inicia · cierra y vuelve a abrir la app",false);lastReport=now;}return;}
            if(!video){video=ar.drawVideoInit(0);if(!video)return;settings();}
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glDisable(GLES20.GL_CULL_FACE);ar.drawVideo(0);
            if(now-lastFrame>600)visible=false;
            if(now-lastReport>400){report(visible?"Marcador detectado · toca una pieza y su destino":"Busca el marcador completo · luz uniforme y sin reflejos",visible);lastReport=now;}
            if(!visible){tapX=-1;return;}
            projection=ar.getProjectionMatrix(5,20000);if(projection==null)return;
            GLES20.glViewport(vp[0],vp[1],vp[2],vp[3]);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDisable(GLES20.GL_BLEND);GLES20.glUseProgram(program);
            float scale=boardScale;float cell=100*scale/8;
            draw(cube,0,0,-3,100*scale+6,100*scale+6,3,.12f,.17f,.22f);
            Chess b;int selected;List<Chess.Move> moves;
            synchronized(game){b=game.position();selected=game.selection();moves=selected>=0?game.moves():Collections.emptyList();}
            Set<Integer> legal=new HashSet<>();for(Chess.Move m:moves)if(m.from==selected)legal.add(m.to);
            for(int s=0;s<64;s++){
                float x=(s%8-3.5f)*cell,y=(s/8-3.5f)*cell;
                boolean light=((s%8+s/8)%2)==1;float r=light?.82f:.16f,g=light?.85f:.28f,bl=light?.79f:.32f;
                if(s==selected){r=.95f;g=.66f;bl=.14f;}else if(legal.contains(s)){r=.23f;g=.64f;bl=.47f;}
                draw(cube,x,y,0,cell*.99f,cell*.99f,.7f,r,g,bl);
                char p=b.board[s];if(p!='.'&&showPieces){boolean white=Chess.isWhite(p);float size=cell/20;
                    draw(pieces.get(Character.toLowerCase(p)),x,y,.8f,size,size,size,white?.96f:.17f,white?.89f:.20f,white?.70f:.28f);}
            }
            float tx=tapX;if(tx>=0){float ty=tapY;tapX=-1;int sq=pick(tx,ty,cell);if(sq>=0)listener.squareTapped(sq);}
        }catch(Throwable e){failed=true;report("Error de cámara o gráficos: "+e.getMessage(),false);}
    }
    private int pick(float x,float y,float cell){
        float nx=2*(x-vp[0])/vp[2]-1,ny=2*((h-y)-vp[1])/vp[3]-1;if(Math.abs(nx)>1||Math.abs(ny)>1)return -1;
        float[] mvp=new float[16],inv=new float[16],a=new float[4],b=new float[4];Matrix.multiplyMM(mvp,0,projection,0,pose,0);if(!Matrix.invertM(inv,0,mvp,0))return -1;
        Matrix.multiplyMV(a,0,inv,0,new float[]{nx,ny,-1,1},0);Matrix.multiplyMV(b,0,inv,0,new float[]{nx,ny,1,1},0);
        for(int i=0;i<3;i++){a[i]/=a[3];b[i]/=b[3];}float dz=b[2]-a[2];if(Math.abs(dz)<.0001)return -1;float t=(.7f-a[2])/dz;if(t<0||t>1)return -1;
        int f=(int)Math.floor((a[0]+t*(b[0]-a[0]))/cell+4),r=(int)Math.floor((a[1]+t*(b[1]-a[1]))/cell+4);return f>=0&&f<8&&r>=0&&r<8?r*8+f:-1;
    }
    private void draw(Mesh mesh,float x,float y,float z,float sx,float sy,float sz,float r,float g,float b){float[] model=new float[16],mv=new float[16],mvp=new float[16];Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mv,0,pose,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,mv,0);GLES20.glUniformMatrix4fv(locMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(locModel,1,false,model,0);GLES20.glUniform4f(locColor,r,g,b,1);mesh.draw(locPos,locNorm);}
    void close(){visible=false;try{if(video&&ar!=null)ar.drawVideoFinal(0);video=false;if(initialized&&ar!=null)ar.stopAndFinal();}finally{initialized=false;}}
    private static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
    private static int makeProgram(){String vs="uniform mat4 uMvp; uniform mat4 uModel; attribute vec3 aPosition; attribute vec3 aNormal; varying float vLight; void main(){gl_Position=uMvp*vec4(aPosition,1.0); vec3 n=normalize(mat3(uModel)*aNormal);vLight=0.48+0.52*max(dot(n,normalize(vec3(-0.4,-0.6,1.0))),0.0);}";String fs="precision mediump float;uniform vec4 uColor;varying float vLight;void main(){gl_FragColor=vec4(uColor.rgb*vLight,uColor.a);}";int p=GLES20.glCreateProgram();int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs);GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);return p;}
}
