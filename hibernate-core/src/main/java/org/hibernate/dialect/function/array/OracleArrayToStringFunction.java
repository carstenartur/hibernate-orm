/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle array_to_string function.
 */
public class OracleArrayToStringFunction extends ArrayToStringFunction {

	public OracleArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				( (Expression) sqlAstArguments.get( 0 ) ).getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_to_string(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		if ( sqlAstArguments.size() > 2 ) {
			sqlAppender.append( ',' );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		else {
			sqlAppender.append( ",null" );
		}
		sqlAppender.append( ')' );
	}
}
