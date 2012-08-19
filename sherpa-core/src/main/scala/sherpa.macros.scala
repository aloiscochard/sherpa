//  __..              
// (__ |_  _ ._.._  _.
// .__)[ )(/,[  [_)(_]
//              |     
//
//  (c) 2012, Alois Cochard
package sherpa

import scala.reflect.makro.Context
import language.experimental.macros

// TODO Refactoring needed to avoid code duplication
// TODO Improve retriving of case class fields, actually very HACKY
// TODO Improve generation of tree form type (ask Eugene) actually *horribly* HACKY
// TODO Support case class with fields having type parameter (i.e. List[])

object Mapper {
  def apply[T] = macro Macro._mapperOf[T]
}

object Macro {
  def mapperOf[T]: Persistable[T, EntityGenerator[String], EntityExtractor[String]] = macro _mapperOf[T]
  
  def _mapperOf[T : ctx.TypeTag](ctx: Context) = {
    import ctx.universe._
    
    val w = _writerOf[T](ctx)
    val r = _readerOf[T](ctx)

    reify {
      new Persistable[T, EntityGenerator[String], EntityExtractor[String]] {
        implicit val writer: Writer[T, EntityGenerator[String]] = w.splice
        implicit val reader: Reader[T, EntityExtractor[String]] = r.splice
      }
    }
  }

  def writerOf[T]: Writer[T, EntityGenerator[String]] = macro _writerOf[T]

  def _writerOf[T : ctx.TypeTag](ctx: Context): ctx.Expr[Writer[T, EntityGenerator[String]]] = {
    import ctx.universe._
    import ctx.universe.Flag._

    val tpe = typeOf[T]

    val values = tpe.nonPrivateMembers.filter(_.kind == "value").map(s => s -> s.typeSignature).collect {
      case (s, _ @ NullaryMethodType(t)) => s.name -> t
    }.toList.reverse

    val valuesName = values.map(_._1)
    val valuesSerialization = values.map { case (name, tpe) =>
      Apply(
        Apply(Select(Ident(newTermName("fs")), newTermName("writeField")), List(Literal(Constant(name.toString)))), 
        List(
          Function(
            List(ValDef(Modifiers(PARAM), newTermName("s"), TypeTree(), EmptyTree)),
            Apply(
              Apply(
                //TypeApply(Select(Ident(newTermName("s")), newTermName("write")), List(Ident(tpe.typeSymbol))), 
                TypeApply(Select(Ident(newTermName("s")), newTermName("write")), parseType(ctx)(tpe.toString)), 
                List(Select(Ident(newTermName("value")), newTermName(name.toString)))
              ), 
              List(
                ctx.inferImplicitValue(appliedType(typeOf[Writer[_, _]], List(tpe, typeOf[EntityGenerator[String]])))
              )
            )
          )
        )
      )
    }
    val unit = Literal(Constant(()))

    val xs = valuesName.map(_.toString).zip(valuesSerialization)
    def cons(xs: List[(String, Tree)], termName: String): Tree = xs match {
      case Nil => Function(
        List(ValDef(Modifiers(PARAM), newTermName(termName), TypeTree(), EmptyTree)),
        unit
      )
      case head :: tail => {
        val (label, tree) = head
        Function(
          List(ValDef(Modifiers(PARAM), newTermName(termName), TypeTree(), EmptyTree)),
          Apply(
            Select(
              tree,
              tail match {
                case Nil => newTermName("map")
                case _ => newTermName("flatMap")
              }
            ), 
            List(cons(tail, label))
          )
        )
      }
    }
    val tree = Function(
      List(ValDef(Modifiers(PARAM), newTermName("value"), Ident(tpe.typeSymbol), EmptyTree)),
      Function(
        List(
          ValDef(
            Modifiers(PARAM),
            newTermName("es"),
            AppliedTypeTree(Ident(typeOf[EntityGenerator[_]].typeSymbol), List(Ident(newTypeName("String")))),
            EmptyTree
          )
        ), 
        Apply(
          Select(Ident(newTermName("es")), newTermName("writeObject")),
          List(
            cons(xs, "fs")
          )
        )
      )
    )

    ctx.Expr[Writer[T, EntityGenerator[String]]](ctx.resetAllAttrs(tree))
  }

  def readerOf[T]: Reader[T, EntityExtractor[String]] = macro _readerOf[T]

  def _readerOf[T : ctx.TypeTag](ctx: Context): ctx.Expr[Reader[T, EntityExtractor[String]]] = {
    import ctx.universe._
    import ctx.universe.Flag._

    val tpe = typeOf[T]

    val values = tpe.nonPrivateMembers.filter(_.kind == "value").map(s => s -> s.typeSignature).collect {
      case (s, t @ NullaryMethodType(tpe)) => s.name -> tpe
    }.toList.reverse

    val valuesName = values.map(_._1)
    val valuesSerialization = values.map { case (name, tpe) =>
      Apply(
        Apply(
          Select(Ident(newTermName("fs")), newTermName("readField")),
          List(Literal(Constant(name.toString)))
        ),
        List(
          Function(
            List(
              ValDef(Modifiers(PARAM), newTermName("s"), TypeTree(), EmptyTree)
            ),
            Apply(
              //TypeApply(Select(Ident(newTermName("s")), newTermName("read")),List(Ident(tpe.typeSymbol))),
              TypeApply(Select(Ident(newTermName("s")), newTermName("read")), parseType(ctx)(tpe.toString)),
              List(
                ctx.inferImplicitValue(appliedType(typeOf[Reader[_, _]], List(tpe, typeOf[EntityExtractor[String]])))
              )
            )
          )
        )
      )
    }

    val create = Apply(
      Select(Ident(tpe.typeSymbol.companionSymbol), newTermName("apply")),
      valuesName.map(name => Ident(newTermName(name.toString)))
    )

    val xs = valuesName.map(_.toString).zip(valuesSerialization)
    def cons(xs: List[(String, Tree)], termName: String): Tree = xs match {
      case Nil => Function(
        List(ValDef(Modifiers(PARAM), newTermName(termName), TypeTree(), EmptyTree)),
        create
      )
      case head :: tail => {
        val (label, tree) = head
        Function(
          List(ValDef(Modifiers(PARAM), newTermName(termName), TypeTree(), EmptyTree)),
          Apply(
            Select(
              tree,
              tail match {
                case Nil => newTermName("map")
                case _ => newTermName("flatMap")
              }
            ), 
            List(cons(tail, label))
          )
        )
      }
    }

    val tree = Function(
      List(
        ValDef(
          Modifiers(PARAM),
          newTermName("es"),
          AppliedTypeTree(
            Ident(typeOf[EntityExtractor[_]].typeSymbol),
            List(
              Ident(typeOf[String].typeSymbol)
            )
          ),
          EmptyTree
        )
      ), 
      Apply(
        Select(
          Ident(newTermName("es")),
          newTermName("readObject")
        ), 
        List(
          cons(xs, "fs")
        )
      )
    )
    ctx.Expr[Reader[T, EntityExtractor[String]]](ctx.resetAllAttrs(tree))
  }

  private def parseType(ctx: Context)(tpe: String): List[ctx.Tree] = {
    import ctx.universe._

    val x = tpe.indexOf("[")
    if (x > 0) {
      val root = tpe.substring(0, x)
      val parameters = tpe.substring(x + 1, tpe.lastIndexOf("]"))
      List(
        AppliedTypeTree(
          Ident(newTypeName(root)),
          parseType(ctx)(parameters)
        )
      )
    } else tpe.split(",").toList.map(name => Ident(newTypeName(name.trim())))
  }

}
