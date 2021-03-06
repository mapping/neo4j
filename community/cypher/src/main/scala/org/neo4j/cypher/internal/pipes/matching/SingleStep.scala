/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.pipes.matching

import org.neo4j.graphdb.{Node, Relationship, Direction, RelationshipType}
import org.neo4j.cypher.internal.commands.{And, Predicate}
import collection.JavaConverters._
import org.neo4j.cypher.internal.pipes.ExecutionContext

case class SingleStep(id: Int,
                      typ: Seq[String],
                      direction: Direction,
                      next: Option[ExpanderStep],
                      relPredicate: Predicate,
                      nodePredicate: Predicate) extends ExpanderStep {

  def createCopy(next: Option[ExpanderStep], direction: Direction, nodePredicate: Predicate): ExpanderStep =
    copy(next = next, direction = direction, nodePredicate = nodePredicate)


  private def filter(r: Relationship, n: Node, parameters: ExecutionContext): Boolean = {
    val m = new MiniMap(r, n, parameters.state)
    relPredicate.isMatch(m) && nodePredicate.isMatch(m)
  }

  def expand(node: Node, parameters: ExecutionContext): (Iterable[Relationship], Option[ExpanderStep]) = {
    val intermediate = parameters.state.query.getRelationshipsFor(node, direction, typ:_*).asScala

    val rels = new FilteringIterable(intermediate, node, And(relPredicate, nodePredicate), parameters)
    (rels, next)
  }

  override def toString = {
    val left =
      if (direction == Direction.OUTGOING)
        ""
      else
        "<"

    val right =
      if (direction == Direction.INCOMING)
        ""
      else
        ">"

    val relInfo = typ.toList match {
      case List() => ""
      case _      => "[:%s {%s,%s}]".format(typ.mkString("|"), relPredicate, nodePredicate)
    }

    val shape = "(%s)%s-%s-%s".format(id, left, relInfo, right)

    next match {
      case None    => "%s()".format(shape)
      case Some(x) => shape + x.toString
    }
  }

  def size: Option[Int] = next match {
    case None    => Some(1)
    case Some(n) => n.size.map(_ + 1)
  }

  override def equals(p1: Any) = p1 match {
    case null                => false
    case other: ExpanderStep =>
      val a = id == other.id
      val b = direction == other.direction
      val c = next == other.next
      val d = typ == other.typ
      val e = relPredicate == other.relPredicate
      val f = nodePredicate == other.nodePredicate
      a && b && c && d && e && f
    case _                   => false
  }

  def shouldInclude() = false
}