package com.siyeh.igtest.controlflow.double_negation;

public class DoubleNegation {

  void negative(boolean b1, boolean b2, boolean b3) {
    boolean r1 = <warning descr="Double negation in '!(b1 != b2)'">!(b1 != b2)</warning>;
    boolean r2 = <warning descr="Double negation in '!!b1'">!!b1</warning>;
    boolean r3 = <warning descr="Double negation in '!b1 != b2'">!b1 != b2</warning>;
    boolean r4 = (<warning descr="Double negation in 'b1 != (b2 != b3)'">b1 != (b2 != b3)</warning>);
    boolean r5 = (<warning descr="Double negation in 'b1 != b2 != b3'">b1 != b2 != b3</warning>);
  }
}
