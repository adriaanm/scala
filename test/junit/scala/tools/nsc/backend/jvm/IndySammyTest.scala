package scala.tools.nsc
package backend.jvm
package opt

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import scala.collection.generic.Clearable
import scala.tools.asm.Opcodes._
import org.junit.Assert._

import scala.tools.asm.tree._
import scala.tools.nsc.reporters.StoreReporter

import CodeGenTools._
import scala.tools.partest.ASMConverters
import ASMConverters._
import AsmUtils._

import BackendReporting._

import scala.collection.convert.decorateAsScala._
import scala.tools.testing.ClearAfterClass

object IndySammyTest extends ClearAfterClass.Clearable {
  var compiler = newCompiler()

  def compile(scalaCode: String, javaCode: List[(String, String)] = Nil, allowMessage: StoreReporter#Info => Boolean = _ => false): List[ClassNode] =
    compileClasses(compiler)(scalaCode, javaCode, allowMessage)

  def clear(): Unit = { compiler = null }
}

// TODO
//@RunWith(classOf[JUnit4])
//class IndySammyTest extends ClearAfterClass {
//  ClearAfterClass.stateToClear = IndySammyTest
//  import IndySammyTest._
//
//  import compiler.genBCode.bTypes._
//  import compiler.genBCode.bTypes.backendUtils._
//
//  val inlineOnlyCompiler = IndySammyTest.inlineOnlyCompiler
//
//  def funName(from: String, to: String) = s"Fun$from$to"
//  def lamName(from: String, to: String) = s"lam$from$to"
//  def appName(from: String, to: String, actual: String) = s"app$from$to_$actual"
//  def classPrologue(from: String, to: String) =
//    s"class VC(private val i: Int) extends AnyVal\ntrait ${funName(from, to)} { def apply(a: $from): $to}"
//
//  def lamDef(from: String, to: String, body: String => String) =
//    s"""def ${lamName(from, to)} = (x => ${body("x")}): ${funName(from, to)}"""
//
//  def appDef(from: String, to: String, arg: String = "new VC(1)", argTp: String) =
//    s"""def ${appName(from, to, argTp)} = ${lamName(from, to)}($arg)"""
//
//  def test(from: String, to: String, arg: String, argTp: String, body: String => String = x => x): (Method, Method) = {
//    compile($"{classPrologue(from, to}; ${lamDef(from, to, body)}; ${appDef(from, to, arg, argTp)}")
//    find methods  lamName(from, to) appName(from, to, argTp)
//  }
//
//}

//trait FunVC_VC   { def apply(a: VC): VC   } // int apply(int var1)
//trait FunInt_VC  { def apply(a: Int): VC  } // int apply(int var1)
//trait FunAny_VC  { def apply(a: Any): VC  } // int apply(Object var1)
//trait FunVC_Any  { def apply(a: VC): Any  } // Object apply(int var1)
//trait FunVC_Int  { def apply(a: VC): Int  } // int apply(int var1)
//trait FunVC_Unit { def apply(a: VC): Unit } // void apply(int var1)
//
//
//class C {
//  def fun_vc_vc = (x => x): FunVC_VC
//  def app_vc_vc__vc = fun_vc_vc(new VC(1))
//
//  def fun_vc_any = (x => x): FunVC_Any
//  def app_vc_any__vc = fun_vc_any(new VC(1))
//
//  def fun_any_vc = (x => new VC(x.asInstanceOf[Int])): FunAny_VC
//  def app_vc_any__I = fun_any_vc(1)
//
//  def fun_int_vc = (x => new VC(x)): FunInt_VC
//  def app_int_vc__I = fun_int_vc(1)
//
//  def fun_vc_int = (x => 1): FunVC_Int
//  def app_vc_int__vc = fun_vc_int(new VC(1))
//
//  def fun_vc_unit = (x => x): FunVC_Unit
//  def app_vc_unit__vc = fun_vc_unit(new VC(1))
//}

// TODO: turn into junit test that checks this compiles to the corresponding java code:
// FunAny_VC  fun_any_vc()   { return x -> BoxesRunTime.unboxToInt((Object)x); }
// FunInt_VC  fun_int_vc()   { return x -> x;                                  }
// FunVC_Any  fun_vc_any()   { return x -> new VC(x);                          }
// FunVC_Int  fun_vc_int()   { return x -> 1;                                  }
// FunVC_Unit fun_vc_unit()  { return x -> { }                                 }
// FunVC_VC   fun_vc_vc()    { return x -> x;                                  }
//
// int    app_vc_any__I()    { return this.fun_any_vc().apply(BoxesRunTime.boxToInteger((int)1));
// int    app_int_vc__I()    { return this.fun_int_vc().apply(1);
// Object app_vc_any__vc()   { return this.fun_vc_any().apply(1);
// int    app_vc_int__vc()   { return this.fun_vc_int().apply(1);
// void   app_vc_unit__vc()  { this.fun_vc_unit().apply(1);
// int    app_vc_vc__vc()    { return this.fun_vc_vc().apply(1);
