/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.COMPARABLE;

/**
 * @see CaseLeastGreatestEmulation
 *
 * @author Christian Beikov
 */
public class QuantifiedLeastGreatestEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String operator;

	public QuantifiedLeastGreatestEmulation(boolean least) {
		super(
				least ? "least" : "greatest",
				new ArgumentTypesValidator( StandardArgumentsValidators.min( 2 ), COMPARABLE, COMPARABLE ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull(),
				StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE
		);
		this.operator = least ? "<=" : ">=";
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		if ( numberOfArguments > 1 ) {
			final int lastArgument = numberOfArguments - 1;
			sqlAppender.appendSql( "case" );
			for ( int i = 0; i < lastArgument; i++ ) {
				sqlAppender.appendSql( " when " );
				arguments.get( i ).accept( walker );
				sqlAppender.appendSql( operator );
				sqlAppender.appendSql( "all(" );
				String separator = "";
				for ( int j = i + 1; j < numberOfArguments; j++ ) {
					sqlAppender.appendSql( separator );
					arguments.get( j ).accept( walker );
					separator = ",";
				}
				sqlAppender.appendSql( ") then " );
				arguments.get( i ).accept( walker );
			}
			sqlAppender.appendSql( " else " );
			arguments.get( lastArgument ).accept( walker );
			sqlAppender.appendSql( " end" );
		}
		else {
			arguments.get( 0 ).accept( walker );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(COMPARABLE arg0[, COMPARABLE arg1[, ...]])";
	}
}
