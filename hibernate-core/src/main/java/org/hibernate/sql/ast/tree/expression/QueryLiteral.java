/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * Represents a literal in the SQL AST.  This form accepts a {@link BasicValuedMapping}
 * as its {@link org.hibernate.metamodel.mapping.MappingModelExpressible}.
 *
 * @author Steve Ebersole
 * @see JdbcLiteral
 */
public class QueryLiteral<T> implements Literal, DomainResultProducer<T> {
	private final T value;
	private final BasicValuedMapping type;

	public QueryLiteral(T value, BasicValuedMapping type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public T getLiteralValue() {
		return value;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type.getJdbcMapping();
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitQueryLiteral( this );
	}

	@Override
	public BasicValuedMapping getExpressionType() {
		return type;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = creationState.getSqlAstCreationState().getSqlExpressionResolver()
				.resolveSqlSelection(
						this,
						type.getJdbcMapping().getJdbcJavaType(),
						null,
						creationState.getSqlAstCreationState()
								.getCreationContext()
								.getSessionFactory()
								.getTypeConfiguration()
				);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				type.getJdbcMapping()
		);
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) throws SQLException {
		//noinspection unchecked
		type.getJdbcMapping().getJdbcValueBinder().bind(
				statement,
				getLiteralValue(),
				startPosition,
				executionContext.getSession()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				type.getJdbcMapping().getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}
}
