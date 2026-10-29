package com.example.archess;
import java.util.*;

/** Small offline opponent, not Stockfish. Depth-limited alpha-beta. */
public final class BasicEngine {
    private static int value(char p){switch(Character.toLowerCase(p)){case 'p':return 100;case 'n':return 320;case 'b':return 330;case 'r':return 500;case 'q':return 900;default:return 0;}}
    private static int eval(Chess b){int n=0;for(int i=0;i<64;i++){char p=b.board[i];if(p=='.')continue;int center=14-Math.abs(2*(i%8)-7)-Math.abs(2*(i/8)-7);int bonus=Character.toLowerCase(p)=='p'?(Chess.isWhite(p)?i/8:7-i/8)*5:Character.toLowerCase(p)=='k'?0:center*2;n+=(Chess.isWhite(p)?1:-1)*(value(p)+bonus);}return b.white?n:-n;}
    private static int search(Chess b,int d,int alpha,int beta){
        if(Thread.currentThread().isInterrupted())return 0;
        List<Chess.Move> moves=b.legal();if(moves.isEmpty())return b.check(b.white)?-100000-d:0;if(b.insufficient()||b.half>=150)return 0;if(d==0)return eval(b);
        moves.sort((a,c)->Integer.compare(value(b.board[c.to]),value(b.board[a.to])));
        for(Chess.Move m:moves){Chess next=b.copy();next.apply(m);int v=-search(next,d-1,-beta,-alpha);if(v>=beta)return beta;if(v>alpha)alpha=v;}return alpha;
    }
    public static Chess.Move choose(Chess b,int depth){Chess.Move best=null;int score=-1000000;for(Chess.Move m:b.legal()){if(Thread.currentThread().isInterrupted())return null;Chess c=b.copy();c.apply(m);int s=-search(c,depth-1,-1000000,-score);if(s>score){score=s;best=m;}}return best;}
}
