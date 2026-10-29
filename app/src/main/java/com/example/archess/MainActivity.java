package com.example.archess;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity implements ChessRenderer.Listener {
    private final Game game=new Game();private FrameLayout root;private LinearLayout hud;private TextView track,status;
    private GLSurfaceView gl;private ChessRenderer renderer;private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> aiJob;
    private boolean active=false,thinking=false;private int aiToken=0;private float scale=1.6f;private int depth=2;
    private String markerDir;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        game.restore(getPreferences(0).getString("game",game.save()));scale=getPreferences(0).getFloat("scale",1.6f);depth=getPreferences(0).getInt("depth",2);
        root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(12,18,25));setContentView(root);buildHud();
        try{File dir=new File(getFilesDir(),"ar");File data=new File(dir,"Data");if(!data.exists()&&!data.mkdirs())throw new IOException("No se pudo crear Data");try(InputStream in=getAssets().open("Data/selector.patt");OutputStream out=new FileOutputStream(new File(data,"selector.patt"))){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}markerDir=dir.getAbsolutePath();}catch(IOException e){track.setText("Error de archivos: "+e.getMessage());}
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(12),0,dp(12),0);b.setOnClickListener(v->action.run());return b;}
    private void buildHud(){hud=new LinearLayout(this);hud.setOrientation(LinearLayout.VERTICAL);hud.setPadding(dp(14),dp(8),dp(14),dp(8));hud.setBackgroundColor(0xDD111C28);hud.addView(text("AR CHESS  ·  PRUEBA 0.1",17));status=text(game.status(),14);hud.addView(status);track=text("Permite la cámara para escanear el marcador",12);hud.addView(track);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);root.addView(hud,hp);
        HorizontalScrollView sc=new HorizontalScrollView(this);sc.setFillViewport(false);sc.setBackgroundColor(0xDD111C28);LinearLayout controls=new LinearLayout(this);
        controls.addView(button("Nueva",()->new AlertDialog.Builder(this).setTitle("Nueva partida").setMessage("Reemplazará la partida guardada en este teléfono.").setPositiveButton("Dos personas",(d,w)->newGame(false)).setNegativeButton("Contra rival básico",(d,w)->newGame(true)).setNeutralButton("Cancelar",null).show()));
        controls.addView(button("Atrás",()->{cancelAi();game.back();save();refresh();}));controls.addView(button("Adelante",()->{cancelAi();game.forward();save();refresh();}));
        controls.addView(button("Rival juega",()->maybeAi()));
        controls.addView(button("Escala −",()->resize(-.2f)));controls.addView(button("Escala +",()->resize(.2f)));
        controls.addView(button("Solo tablero",()->{if(renderer!=null)renderer.showPieces=!renderer.showPieces;}));
        controls.addView(button("Nivel",()->new AlertDialog.Builder(this).setTitle("Rival básico sin conexión").setSingleChoiceItems(new String[]{"Rápido · profundidad 1","Básico · profundidad 2","Más cálculo · profundidad 3"},depth-1,(d,w)->{cancelAi();depth=w+1;save();d.dismiss();maybeAi();}).setNegativeButton("Cerrar",null).show()));
        controls.addView(button("Tablas",()->{if(game.canClaim()){cancelAi();game.claim();save();refresh();}else toast("Puedes reclamar con tres repeticiones o 50 jugadas sin captura ni movimiento de peón.");}));
        controls.addView(button("Ayuda",()->help()));
        sc.addView(controls);root.addView(sc,new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM));
    }
    private void resize(float d){scale=Math.max(.8f,Math.min(3.2f,scale+d));if(renderer!=null)renderer.boardScale=scale;save();toast(String.format(Locale.US,"Tablero: %.1f veces el lado del marcador",scale));}
    private void newGame(boolean ai){cancelAi();game.reset(ai);save();refresh();}
    private void save(){getPreferences(0).edit().putString("game",game.save()).putFloat("scale",scale).putInt("depth",depth).apply();}
    private void refresh(){status.setText(game.status()+(thinking?" · rival pensando":"")+(game.ai()?" · rival básico":""));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void help(){new AlertDialog.Builder(this).setTitle("Cómo probar AR Chess").setMessage("1. Imprime UN marcador al 100 %, primero el de 10 cm.\n2. Enfócalo completo, con margen blanco y buena luz.\n3. Toca la base de una pieza y luego una casilla verde.\n\nMantén el marcador visible; al perderlo se oculta el tablero y se conserva la partida. La escala es relativa, no una medida en centímetros.\n\nLa app funciona sin Internet. Rival básico experimental; no es Stockfish. Atrás y Adelante revisan el historial; Rival juega continúa el turno automático.\n\nPrueba 0.1: Android 7 o posterior, ARM. Cámara y seguimiento pendientes de validar en tu teléfono. Algunos dispositivos de páginas de 16 KB pueden ser incompatibles.").setPositiveButton("Entendido",null).setNeutralButton("Créditos",(d,w)->credits()).show();}
    private void credits(){new AlertDialog.Builder(this).setTitle("Créditos y licencias").setMessage("Adaptación móvil basada en AR-Chess de Carlos Miguel Correia da Costa (2013), licencia MIT. Patrón selector original.\n\nSeguimiento: artoolkitX 1.1.22, LGPLv3 con excepción de enlace.\n\nReglas, interfaz y modelos geométricos nuevos. Las licencias completas están incluidas en assets/licenses y en el código entregado.\n\ngithub.com/carlosmccosta/AR-Chess\ngithub.com/artoolkitx/artoolkitx").setPositiveButton("Cerrar",null).show();}
    @Override protected void onResume(){super.onResume();active=true;if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();else requestPermissions(new String[]{Manifest.permission.CAMERA},7);}
    private void startCamera(){if(gl!=null||markerDir==null||!active)return;renderer=new ChessRenderer(game,markerDir,this);renderer.boardScale=scale;gl=new GLSurfaceView(this);gl.setEGLContextClientVersion(2);gl.setEGLConfigChooser(8,8,8,8,24,0);gl.setRenderer(renderer);gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);gl.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){renderer.tap(e.getX(),e.getY());v.performClick();}return true;});root.addView(gl,0,new FrameLayout.LayoutParams(-1,-1));gl.onResume();maybeAi();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==7){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startCamera();else{track.setText("Cámara denegada · actívala en Ajustes de Android y vuelve a abrir");new AlertDialog.Builder(this).setTitle("Se necesita la cámara").setMessage("La cámara se usa localmente para reconocer el marcador. No se graba ni se envía video.").setPositiveButton("Abrir ajustes",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).setNegativeButton("Cerrar",null).show();}}}
    @Override protected void onPause(){active=false;cancelAi();save();if(gl!=null){GLSurfaceView old=gl;ChessRenderer oldRenderer=renderer;CountDownLatch done=new CountDownLatch(1);old.queueEvent(()->{try{oldRenderer.close();}finally{done.countDown();}});try{done.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}old.onPause();root.removeView(old);gl=null;renderer=null;}super.onPause();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    @Override public void tracking(String s,boolean visible){runOnUiThread(()->{if(active){track.setText(s);track.setTextColor(visible?0xFF8BE0A8:0xFFFFD188);}});}
    @Override public void squareTapped(int square){runOnUiThread(()->handleTap(square));}
    private void handleTap(int square){if(!active||thinking||!game.result().isEmpty())return;if(game.aiTurn()){maybeAi();return;}Chess b=game.position();int from=game.selection();List<Chess.Move> candidates=new ArrayList<>();for(Chess.Move m:game.moves())if(m.from==from&&m.to==square)candidates.add(m);
        if(!candidates.isEmpty()){if(candidates.size()>1){final int rev=game.revision();new AlertDialog.Builder(this).setTitle("Promoción de peón").setItems(new String[]{"Dama","Torre","Alfil","Caballo"},(d,w)->{if(game.revision()==rev)commit(candidates.get(w));}).show();}else commit(candidates.get(0));return;}
        char p=b.board[square];if(p!='.'&&Chess.isWhite(p)==b.white){game.select(square==from?-1:square);status.setText(game.status()+" · "+pieceName(p)+" "+Chess.square(square));}else {game.select(-1);refresh();}
    }
    private static String pieceName(char p){switch(Character.toLowerCase(p)){case 'p':return "peón";case 'r':return "torre";case 'n':return "caballo";case 'b':return "alfil";case 'q':return "dama";default:return "rey";}}
    private void commit(Chess.Move m){if(game.move(m)){save();refresh();maybeAi();}}
    private void cancelAi(){aiToken++;if(aiJob!=null)aiJob.cancel(true);aiJob=null;thinking=false;}
    private void maybeAi(){if(!active||thinking||!game.aiTurn()||!game.atEnd())return;thinking=true;refresh();final int token=++aiToken,rev=game.revision(),d=depth;Chess position=game.position();aiJob=worker.submit(()->{Chess.Move m=BasicEngine.choose(position,d);runOnUiThread(()->{if(token!=aiToken||!active)return;thinking=false;if(rev==game.revision()&&m!=null&&game.aiTurn()){game.move(m);save();}refresh();});});}
}
