package com.witchersbestiary.client.tracking;
import com.witchersbestiary.contracts.tracking.ContractClue.Type;
import net.minecraft.util.math.Box;
import java.util.*;
/** Original authored shallow solid geometry, not sprites or copied textures. All shapes are cached. */
public final class ClueVisuals {
 private static final Map<Type,List<Box>> SHAPES=new EnumMap<>(Type.class);
 static {
  var prints=new ArrayList<Box>();for(double x:new double[]{.2,.58}){prints.add(new Box(x,.012,.37,x+.18,.03,.7));for(int toe=0;toe<3;toe++)prints.add(new Box(x+toe*.065,.012,.25,x+toe*.065+.045,.028,.35));}SHAPES.put(Type.FOOTPRINTS,List.copyOf(prints));
  SHAPES.put(Type.BLOOD_TRACE,List.of(new Box(.22,.012,.28,.61,.022,.62),new Box(.4,.012,.44,.78,.024,.7),new Box(.16,.012,.66,.28,.022,.78),new Box(.68,.012,.2,.77,.022,.3)));
  SHAPES.put(Type.CLAW_MARKS,List.of(new Box(.2,.015,.2,.24,.032,.8),new Box(.42,.015,.15,.46,.032,.85),new Box(.64,.015,.22,.68,.032,.77)));
  SHAPES.put(Type.REMAINS,List.of(new Box(.17,.04,.4,.78,.1,.47),new Box(.19,.02,.38,.29,.15,.5),new Box(.67,.02,.37,.79,.15,.5),new Box(.44,.07,.18,.51,.14,.8),new Box(.4,.04,.17,.55,.18,.29),new Box(.4,.04,.68,.55,.18,.82)));
  SHAPES.put(Type.MIST_RESIDUE,List.of(new Box(.18,.03,.26,.5,.19,.61),new Box(.37,.13,.34,.75,.39,.7),new Box(.3,.32,.43,.59,.54,.68)));
 }
 public static List<Box> shapes(Type type){return SHAPES.get(type);}
 public static float[] color(Type type){return switch(type){case FOOTPRINTS->new float[]{.16f,.33f,.3f};case BLOOD_TRACE->new float[]{.55f,.045f,.025f};case CLAW_MARKS->new float[]{.65f,.35f,.14f};case REMAINS->new float[]{.85f,.79f,.59f};case MIST_RESIDUE->new float[]{.57f,.77f,.88f};};}
}
