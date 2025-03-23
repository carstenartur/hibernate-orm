/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementation for handling binary literals
 *
 * @author Gavin King
 */
public class JdbcLiteralFormatterBinary<T> extends BasicJdbcLiteralFormatter<T> {
	public JdbcLiteralFormatterBinary(JavaType<T> javaType) {
		super( javaType );
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		dialect.appendBinaryLiteral( appender, unwrap( value, byte[].class, wrapperOptions ) );
	}
}
