/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Specialized SemanticQueryWalker (SQM visitor) for producing SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqmToSqlAstConverter extends SemanticQueryWalker<Object>, SqlAstCreationState {
	Stack<Clause> getCurrentClauseStack();

	SqmQueryPart<?> getCurrentSqmQueryPart();

	void registerQueryTransformer(QueryTransformer transformer);

	/**
	 * Returns the function return type implied from the context within which it is used.
	 * If there is no current function being processed or no context implied type, the return is <code>null</code>.
	 */
	MappingModelExpressible<?> resolveFunctionImpliedReturnType();

	MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression);

	/**
	 * Visits the given node with the given inferred type access.
	 */
	Object visitWithInferredType(SqmVisitableNode node, Supplier<MappingModelExpressible<?>> inferredTypeAccess);

	List<Expression> expandSelfRenderingFunctionMultiValueParameter(SqmParameter<?> sqmParameter);

	Predicate visitNestedTopLevelPredicate(SqmPredicate predicate);

}
