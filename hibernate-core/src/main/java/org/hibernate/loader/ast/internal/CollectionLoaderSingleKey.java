/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * Main implementation of CollectionLoader for handling a load of a single collection-key
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderSingleKey implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;

	private final int keyJdbcCount;

	private final SelectStatement sqlAst;
	private final List<JdbcParameter> jdbcParameters;

	public CollectionLoaderSingleKey(
			PluralAttributeMapping attributeMapping,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;

		this.keyJdbcCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();

		this.jdbcParameters = new ArrayList<>();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				1,
				influencers,
				LockOptions.NONE,
				jdbcParameters::add,
				sessionFactory
		);
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return getAttributeMapping();
	}

	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	public List<JdbcParameter> getJdbcParameters() {
		return jdbcParameters;
	}

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), key );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final JdbcOperationQuerySelect jdbcSelect = sqlAstTranslatorFactory
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				jdbcParameters,
				jdbcParameterBindings
		);

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new CollectionLoaderSingleKeyExecutionContext( session, collectionKey, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private static class CollectionLoaderSingleKeyExecutionContext extends BaseExecutionContext {
		private final CollectionKey collectionKey;
		private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

		CollectionLoaderSingleKeyExecutionContext(
				SharedSessionContractImplementor session,
				CollectionKey collectionKey,
				SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
			super( session );
			this.collectionKey = collectionKey;
			this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		}

		@Override
		public CollectionKey getCollectionKey() {
			return collectionKey;
		}

		@Override
		public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
			subSelectFetchableKeysHandler.addKey( entityKey, entry );
		}

	}
}
