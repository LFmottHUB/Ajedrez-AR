package com.example.archess;
public class ChessTest {
    static int assertions=0;
    static void eq(Object a,Object b){assertions++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    static void play(Chess b,String m){Chess.Move move=b.find(m);if(move==null)throw new AssertionError("illegal "+m+" "+b.fen());b.apply(move);}
    public static void main(String[] args){
        Chess b=new Chess();eq(b.fen(),Chess.START);eq(Chess.perft(b,1),20L);eq(Chess.perft(b,2),400L);eq(Chess.perft(b,3),8902L);eq(Chess.perft(b,4),197281L);
        Chess k=new Chess("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");eq(Chess.perft(k,1),48L);eq(Chess.perft(k,2),2039L);eq(Chess.perft(k,3),97862L);
        Chess e=new Chess("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");eq(Chess.perft(e,3),2812L);
        play(b,"f2f3");play(b,"e7e5");play(b,"g2g4");play(b,"d8h4");eq(b.terminal(),"Jaque mate · ganan negras");
        Chess p=new Chess("7k/P7/8/8/8/8/8/7K w - - 0 1");int n=0;for(Chess.Move m:p.legal())if(m.from==48)n++;eq(n,4);play(p,"a7a8n");eq(p.board[56],'N');
        Chess ep=new Chess("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");eq(ep.find("e5d6")==null,true);
        Game g=new Game();g.move(g.position().find("e2e4"));String save=g.save();Game restored=new Game();restored.restore(save);eq(restored.position().fen(),g.position().fen());g.back();eq(g.position().fen(),Chess.START);g.forward();eq(g.position().fen(),restored.position().fen());
        Game rep=new Game();for(int i=0;i<2;i++)for(String m:new String[]{"g1f3","g8f6","f3g1","f6g8"})rep.move(rep.position().find(m));eq(rep.repetitions(),3);eq(rep.canClaim(),true);rep.claim();eq(rep.result(),"Tablas reclamadas");
        Chess ai=new Chess();Chess.Move move=BasicEngine.choose(ai,2);eq(ai.find(move.uci())!=null,true);
        System.out.println("PASS "+assertions+" assertions; perft initial depths 1-4, Kiwipete 1-3, en passant, promotion, mate, history, repetition, AI.");
    }
}
