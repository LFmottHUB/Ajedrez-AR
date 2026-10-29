package com.example.archess;

import java.util.*;

/** Independent rules implementation; board index a1=0, h8=63. No Android dependency. */
public final class Chess {
    public static final String START="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public char[] board=new char[64];
    public boolean white=true;
    public int rights=15, ep=-1, half=0, full=1;
    public Chess(){load(START);}
    public Chess(String fen){load(fen);}
    public Chess copy(){return new Chess(fen());}
    public static boolean isWhite(char p){return p>='A'&&p<='Z';}
    public static String square(int s){return ""+(char)('a'+s%8)+(char)('1'+s/8);}
    public static int square(String s){return s.charAt(0)-'a'+8*(s.charAt(1)-'1');}
    public static final class Move {
        public final int from,to; public final char promotion;
        public Move(int f,int t,char p){from=f;to=t;promotion=p;}
        public String uci(){return square(from)+square(to)+(promotion==0?"":String.valueOf(promotion));}
    }
    public void load(String fen){
        String[] a=fen.split(" "); Arrays.fill(board,'.'); String[] ranks=a[0].split("/");
        if(ranks.length!=8)throw new IllegalArgumentException("FEN ranks");
        for(int r=0;r<8;r++){int f=0;for(char c:ranks[7-r].toCharArray()){if(c>='1'&&c<='8')f+=c-'0';else {if(f>=8||"PNBRQKpnbrqk".indexOf(c)<0)throw new IllegalArgumentException("FEN piece");board[r*8+f++]=c;}}if(f!=8)throw new IllegalArgumentException("FEN width");}
        white=a[1].equals("w");rights=0;for(int i=0;i<4;i++)if(a[2].indexOf("KQkq".charAt(i))>=0)rights|=1<<i;
        ep=a[3].equals("-")?-1:square(a[3]);half=Integer.parseInt(a[4]);full=Integer.parseInt(a[5]);
    }
    public String fen(){StringBuilder s=new StringBuilder();for(int r=7;r>=0;r--){int n=0;for(int f=0;f<8;f++){char p=board[r*8+f];if(p=='.')n++;else{if(n>0){s.append(n);n=0;}s.append(p);}}if(n>0)s.append(n);if(r>0)s.append('/');}s.append(white?" w ":" b ");if(rights==0)s.append('-');else for(int i=0;i<4;i++)if((rights&(1<<i))!=0)s.append("KQkq".charAt(i));return s+" "+(ep<0?"-":square(ep))+" "+half+" "+full;}
    public boolean attacked(int sq,boolean byWhite){
        int x=sq%8,y=sq/8;
        for(int s=0;s<64;s++){char p=board[s];if(p=='.'||isWhite(p)!=byWhite)continue;int dx=x-s%8,dy=y-s/8,ax=Math.abs(dx),ay=Math.abs(dy);p=Character.toLowerCase(p);
            if(p=='p'){if(ay==1&&ax==1&&dy==(byWhite?1:-1))return true;continue;}
            if(p=='n'){if(ax*ay==2)return true;continue;}
            if(p=='k'){if(Math.max(ax,ay)==1)return true;continue;}
            boolean line=(p=='b'&&ax==ay&&ax>0)||(p=='r'&&((dx==0)^(dy==0)))||(p=='q'&&((ax==ay&&ax>0)||((dx==0)^(dy==0))));
            if(line){int stepx=Integer.signum(dx),stepy=Integer.signum(dy),xx=s%8+stepx,yy=s/8+stepy;boolean clear=true;while(xx!=x||yy!=y){if(board[yy*8+xx]!='.'){clear=false;break;}xx+=stepx;yy+=stepy;}if(clear)return true;}
        }return false;
    }
    public boolean check(boolean side){char king=side?'K':'k';for(int s=0;s<64;s++)if(board[s]==king)return attacked(s,!side);return true;}
    private void add(List<Move> a,int f,int t){if(Character.toLowerCase(board[f])=='p'&&(t/8==0||t/8==7))for(char p:new char[]{'q','r','b','n'})a.add(new Move(f,t,p));else a.add(new Move(f,t,(char)0));}
    public List<Move> legal(){
        List<Move> all=new ArrayList<>();
        for(int s=0;s<64;s++){char pc=board[s];if(pc=='.'||isWhite(pc)!=white)continue;char p=Character.toLowerCase(pc);int x=s%8,y=s/8;
            if(p=='p'){int dir=white?1:-1,yy=y+dir;if(yy<0||yy>7)continue;int t=s+dir*8;
                if(board[t]=='.'){add(all,s,t);if(y==(white?1:6)&&board[s+16*dir]=='.')add(all,s,s+16*dir);}
                for(int dx:new int[]{-1,1})if(x+dx>=0&&x+dx<8){t=yy*8+x+dx;char c=board[t];boolean capture=c!='.'&&isWhite(c)!=white&&Character.toLowerCase(c)!='k';boolean en=t==ep&&c=='.'&&board[t-8*dir]==(white?'p':'P');if(capture||en)add(all,s,t);}continue;
            }
            int[][] dirs=p=='n'?new int[][]{{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}}:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
            for(int[] d:dirs){if(p=='b'&&(d[0]==0||d[1]==0))continue;if(p=='r'&&d[0]!=0&&d[1]!=0)continue;
                for(int k=1;k<8;k++){int xx=x+d[0]*k,yy=y+d[1]*k;if(xx<0||xx>7||yy<0||yy>7)break;int t=yy*8+xx;char c=board[t];if(c!='.'&&isWhite(c)==white)break;if(Character.toLowerCase(c)!='k')add(all,s,t);if(c!='.'||p=='n'||p=='k')break;}}
            if(p=='k'&&s==(white?4:60)&&!check(white)){
                int off=white?0:56,rb=white?1:4;char rook=white?'R':'r';
                if((rights&rb)!=0&&board[off+7]==rook&&board[off+5]=='.'&&board[off+6]=='.'&&!attacked(off+5,!white)&&!attacked(off+6,!white))add(all,s,off+6);
                if((rights&(rb*2))!=0&&board[off]==rook&&board[off+1]=='.'&&board[off+2]=='.'&&board[off+3]=='.'&&!attacked(off+3,!white)&&!attacked(off+2,!white))add(all,s,off+2);
            }
        }
        List<Move> ok=new ArrayList<>();for(Move m:all){Chess b=copy();b.apply(m);if(!b.check(white))ok.add(m);}return ok;
    }
    /** Applies an already validated move. */
    public void apply(Move m){char p=board[m.from],c=board[m.to];boolean pawn=Character.toLowerCase(p)=='p';
        if(pawn&&m.to==ep&&c=='.'&&m.from%8!=m.to%8)board[m.to+(white?-8:8)]='.';
        board[m.to]=p;board[m.from]='.';
        if(Character.toLowerCase(p)=='k'){rights&=white?12:3;if(Math.abs(m.to-m.from)==2){int rf=m.to>m.from?m.from+3:m.from-4,rt=m.to>m.from?m.from+1:m.from-1;board[rt]=board[rf];board[rf]='.';}}
        int[] squares={7,0,63,56};for(int i=0;i<4;i++)if(m.from==squares[i]||m.to==squares[i])rights&=~(1<<i);
        if(m.promotion!=0)board[m.to]=white?Character.toUpperCase(m.promotion):m.promotion;
        ep=pawn&&Math.abs(m.to-m.from)==16?(m.from+m.to)/2:-1;half=(pawn||c!='.')?0:half+1;if(!white)full++;white=!white;
    }
    public boolean insufficient(){int minors=0,bishopColor=-1;boolean bishopsOnly=true;for(int s=0;s<64;s++){char p=Character.toLowerCase(board[s]);if(p=='.'||p=='k')continue;if(p=='p'||p=='r'||p=='q')return false;minors++;if(p=='n')bishopsOnly=false;else {int color=(s%8+s/8)%2;if(bishopColor==-1)bishopColor=color;else if(bishopColor!=color)bishopsOnly=false;}}return minors<=1||bishopsOnly;}
    public String terminal(){List<Move> m=legal();if(m.isEmpty())return check(white)?"Jaque mate · ganan "+(white?"negras":"blancas"):"Tablas por ahogado";if(insufficient())return "Tablas por material insuficiente";if(half>=150)return "Tablas por 75 jugadas";return "";}
    public String repetitionKey(){String[] a=fen().split(" ");String en="-";if(ep>=0)for(Move m:legal())if(m.to==ep&&Character.toLowerCase(board[m.from])=='p'&&m.from%8!=m.to%8){en=a[3];break;}return a[0]+" "+a[1]+" "+a[2]+" "+en;}
    public Move find(String uci){for(Move m:legal())if(m.uci().equals(uci))return m;return null;}
    public static long perft(Chess b,int depth){if(depth==0)return 1;long n=0;for(Move m:b.legal()){Chess c=b.copy();c.apply(m);n+=perft(c,depth-1);}return n;}
}
