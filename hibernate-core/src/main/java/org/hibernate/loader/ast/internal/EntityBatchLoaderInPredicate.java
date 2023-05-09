/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.loader.ast.spi.SqlInPredicateMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_DEBUG_ENABLED;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * An EntityBatchLoader using one or more SQL queries, which each initialize up to
 * {@linkplain #getSqlBatchSize()} entities using a SQL IN predicate restriction -
 * e.g., {@code ... where id in (?,?,...)}.
 * <p/>
 * The number of parameters rendered into the SQL is controlled by {@linkplain #getSqlBatchSize()}.
 * Any unused parameter slots for a particular execution are set to {@code null}.
 *
 * @author Steve Ebersole
 */
public class EntityBatchLoaderInPredicate<T>
		extends SingleIdEntityLoaderSupport<T>
		implements EntityBatchLoader<T>, SqlInPredicateMultiKeyLoader, Preparable {
	private final int domainBatchSize;
	private final int sqlBatchSize;

	private List<JdbcParameter> jdbcParameters;
	private SelectStatement sqlAst;
	private JdbcOperationQuerySelect jdbcSelectOperation;

	/**
	 * @param domainBatchSize The maximum number of entities we will initialize for each {@link #load load}
	 * @param sqlBatchSize The number of keys our SQL AST should be able to fetch
	 */
	public EntityBatchLoaderInPredicate(
			int domainBatchSize,
			int sqlBatchSize,
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		this.domainBatchSize = domainBatchSize;
		this.sqlBatchSize = sqlBatchSize;

		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Batch fetching `%s` entity using padded IN-list : %s (%s)",
					entityDescriptor.getEntityName(),
					domainBatchSize,
					sqlBatchSize
			);
		}
	}

	@Override
	public int getDomainBatchSize() {
		return domainBatchSize;
	}

	public int getSqlBatchSize() {
		return sqlBatchSize;
	}

	@Override
	public final T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( pkValue, null, lockOptions, readOnly, session );
	}

	@Override
	public final T load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Batch loading entity `%s#%s`", getLoadable().getEntityName(), pkValue );
		}

		final Object[] idsToInitialize = resolveIdsToLoad( pkValue, session );
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Ids to batch-fetch initialize (`%s#%s`) %s", getLoadable().getEntityName(), pkValue, Arrays.toString(idsToInitialize) );
		}

		initializeEntities( idsToInitialize, pkValue, entityInstance, lockOptions, readOnly, session );

		final EntityKey entityKey = session.generateEntityKey( pkValue, getLoadable().getEntityPersister() );
		//noinspection unchecked
		return (T) session.getPersistenceContext().getEntity( entityKey );
	}

	protected Object[] resolveIdsToLoad(Object pkValue, SharedSessionContractImplementor session) {
		return session.getPersistenceContextInternal().getBatchFetchQueue().getBatchLoadableEntityIds(
				getLoadable(),
				pkValue,
				domainBatchSize
		);
	}

	protected void initializeEntities(
			Object[] idsToInitialize,
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final MultiKeyLoadChunker<Object> chunker = new MultiKeyLoadChunker<>(
				sqlBatchSize,
				getLoadable().getIdentifierMapping().getJdbcTypeCount(),
				getLoadable().getIdentifierMapping(),
				jdbcParameters,
				sqlAst,
				jdbcSelectOperation
		);

		final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();
		final List<EntityKey> entityKeys = arrayList( sqlBatchSize );

		chunker.processChunks(
				idsToInitialize,
				sqlBatchSize,
				(jdbcParameterBindings, session1) -> {
					// Create a RegistrationHandler for handling any subselect fetches we encounter handling this chunk
					final SubselectFetch.RegistrationHandler registrationHandler = SubselectFetch.createRegistrationHandler(
							batchFetchQueue,
							sqlAst,
							jdbcParameters,
							jdbcParameterBindings
					);
					return new SingleIdExecutionContext(
							pkValue,
							entityInstance,
							getLoadable().getRootEntityDescriptor(),
							readOnly,
							lockOptions,
							registrationHandler,
							session
					);
				},
				(key, relativePosition, absolutePosition) -> {
					if ( key != null ) {
						entityKeys.add( session.generateEntityKey( key, getLoadable().getEntityPersister() ) );
					}
				},
				(startIndex) -> {
					if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
						MULTI_KEY_LOAD_LOGGER.debugf(
								"Processing entity batch-fetch chunk (`%s#%s`) %s - %s",
								getLoadable().getEntityName(),
								pkValue,
								startIndex,
								startIndex + ( sqlBatchSize -1)
						);
					}
				},
				(startIndex, nonNullElementCount) -> {
					entityKeys.forEach( batchFetchQueue::removeBatchLoadableEntityKey );
					entityKeys.clear();
				},
				session
		);



//		int numberOfIdsLeft = idsToInitialize.length;
//		int start = 0;
//		while ( numberOfIdsLeft > 0 ) {
//			if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
//				MULTI_KEY_LOAD_LOGGER.debugf( "Processing batch-fetch chunk (`%s#%s`) %s - %s", getLoadable().getEntityName(), pkValue, start, start + ( sqlBatchSize -1) );
//			}
//			initializeChunk( idsToInitialize, start, pkValue, entityInstance, lockOptions, readOnly, session );
//
//			start += sqlBatchSize;
//			numberOfIdsLeft -= sqlBatchSize;
//		}
	}

	private void initializeChunk(
			Object[] idsToInitialize,
			int start,
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		initializeChunk(
				idsToInitialize,
				getLoadable(),
				start,
				sqlBatchSize,
				jdbcParameters,
				sqlAst,
				jdbcSelectOperation,
				pkValue,
				entityInstance,
				lockOptions,
				readOnly,
				session
		);
	}

	private static void initializeChunk(
			Object[] idsToInitialize,
			EntityMappingType entityMapping,
			int startIndex,
			int numberOfKeys,
			List<JdbcParameter> jdbcParameters,
			SelectStatement sqlAst,
			JdbcOperationQuerySelect jdbcSelectOperation,
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final BatchFetchQueue batchFetchQueue = session.getPersistenceContext().getBatchFetchQueue();

		final int numberOfJdbcParameters = entityMapping.getIdentifierMapping().getJdbcTypeCount() * numberOfKeys;
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( numberOfJdbcParameters );

		final List<EntityKey> entityKeys = arrayList( numberOfKeys );
		int bindCount = 0;
		for ( int i = 0; i < numberOfKeys; i++ ) {
			final int idPosition = i + startIndex;
			final Object value;
			if ( idPosition >= idsToInitialize.length ) {
				value = null;
			}
			else {
				value = idsToInitialize[idPosition];
			}
			if ( value != null ) {
				entityKeys.add( session.generateEntityKey( value, entityMapping.getEntityPersister() ) );
			}
			bindCount += jdbcParameterBindings.registerParametersForEachJdbcValue(
					value,
					bindCount,
					entityMapping.getIdentifierMapping(),
					jdbcParameters,
					session
			);
		}
		assert bindCount == jdbcParameters.size();

		if ( entityKeys.isEmpty() ) {
			// there are no non-null keys in the chunk
			return;
		}

		// Create a SubselectFetch.RegistrationHandler for handling any subselect fetches we encounter here
		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				jdbcParameters,
				jdbcParameterBindings
		);

		session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelectOperation,
				jdbcParameterBindings,
				new SingleIdExecutionContext(
						pkValue,
						entityInstance,
						entityMapping.getRootEntityDescriptor(),
						readOnly,
						lockOptions,
						subSelectFetchableKeysHandler,
						session
				),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		entityKeys.forEach( batchFetchQueue::removeBatchLoadableEntityKey );
	}

	@Override
	public void prepare() {
		EntityIdentifierMapping identifierMapping = getLoadable().getIdentifierMapping();

		final int expectedNumberOfParameters = identifierMapping.getJdbcTypeCount() * sqlBatchSize;

		jdbcParameters = arrayList( expectedNumberOfParameters );
		sqlAst = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				identifierMapping,
				null,
				sqlBatchSize,
				LoadQueryInfluencers.NONE,
				LockOptions.NONE,
				jdbcParameters::add,
				sessionFactory
		);
		assert jdbcParameters.size() == expectedNumberOfParameters;

		jdbcSelectOperation = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityBatchLoaderInPredicate(%s [%s (%s)])",
				getLoadable().getEntityName(),
				domainBatchSize,
				sqlBatchSize
		);
	}
}
