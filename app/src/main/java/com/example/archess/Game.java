package com.example.archess;
import java.util.*;

/** All public state access is serialized across UI, render and AI threads. */
public final class Game {
    private final List<String> history=new ArrayList<>();
    private int cursor=0,selected=-1,revision=0;
    private boolean ai=false,claimed=false;
    private Chess chess=new Chess();
    public Game(){history.add(chess.fen());}
    public synchronized Chess position(){return chess.copy();}
    public synchronized int selection(){return selected;}
    public synchronized int revision(){return revision;}
    public synchronized boolean ai(){return ai;}
    public synchronized boolean aiTurn(){return ai&&!chess.white&&result().isEmpty();}
    public synchronized List<Chess.Move> moves(){return chess.legal();}
    public synchronized void select(int s){selected=s;}
    public synchronized void reset(boolean computer){ai=computer;chess=new Chess();cursor=0;history.clear();history.add(chess.fen());selected=-1;claimed=false;revision++;}
    public synchronized boolean move(Chess.Move m){if(!result().isEmpty())return false;Chess.Move legal=chess.find(m.uci());if(legal==null)return false;chess.apply(legal);while(history.size()>cursor+1)history.remove(history.size()-1);history.add(chess.fen());cursor++;selected=-1;claimed=false;revision++;return true;}
    public synchronized void back(){if(cursor>0){cursor--;chess=new Chess(history.get(cursor));selected=-1;claimed=false;revision++;}}
    public synchronized void forward(){if(cursor+1<history.size()){cursor++;chess=new Chess(history.get(cursor));selected=-1;claimed=false;revision++;}}
    public synchronized boolean atEnd(){return cursor==history.size()-1;}
    public synchronized int repetitions(){String key=chess.repetitionKey();int n=0;for(int i=0;i<=cursor;i++)if(new Chess(history.get(i)).repetitionKey().equals(key))n++;return n;}
    public synchronized boolean canClaim(){return chess.half>=100||repetitions()>=3;}
    public synchronized void claim(){if(canClaim()){claimed=true;revision++;}}
    public synchronized String result(){String t=chess.terminal();if(!t.isEmpty())return t;if(claimed)return "Tablas reclamadas";if(repetitions()>=5)return "Tablas por cinco repeticiones";return "";}
    public synchronized String status(){String r=result();return r.isEmpty()?(chess.white?"Turno de blancas":"Turno de negras")+(chess.check(chess.white)?" · JAQUE":"")+" · "+cursor+" medias jugadas":r;}
    public synchronized String save(){StringBuilder s=new StringBuilder();s.append(ai?1:0).append(' ').append(cursor).append(' ').append(claimed?1:0).append('\n');for(String f:history)s.append(f).append('\n');return s.toString();}
    public synchronized void restore(String s){try{String[] a=s.split("\n"),head=a[0].split(" ");List<String> loaded=new ArrayList<>();for(int i=1;i<a.length;i++){new Chess(a[i]);loaded.add(a[i]);}int c=Integer.parseInt(head[1]);if(c<0||c>=loaded.size())return;ai=head[0].equals("1");claimed=head.length>2&&head[2].equals("1");history.clear();history.addAll(loaded);cursor=c;chess=new Chess(history.get(c));selected=-1;revision++;}catch(Exception ignored){reset(false);}}
}
