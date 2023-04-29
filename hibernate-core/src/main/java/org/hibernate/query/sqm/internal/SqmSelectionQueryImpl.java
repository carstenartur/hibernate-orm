/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Tuple;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.internal.SqmInterpretationsKey.InterpretationsKeySource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.results.internal.TupleMetadata;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;

/**
 * @author Steve Ebersole
 */
public class SqmSelectionQueryImpl<R> extends AbstractSelectionQuery<R> implements SqmSelectionQuery<R>, InterpretationsKeySource {
	private final String hql;
	private final SqmSelectStatement<R> sqm;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindingsImpl parameterBindings;

	private final Class<R> resultType;
	private final TupleMetadata tupleMetadata;

	public SqmSelectionQueryImpl(
			String hql,
			HqlInterpretation hqlInterpretation,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;

		//noinspection unchecked
		this.sqm = (SqmSelectStatement<R>) hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

//		visitQueryReturnType( sqm.getQueryPart(), expectedResultType, getSessionFactory() );
		this.resultType = determineResultType( sqm, expectedResultType );

		setComment( hql );
		this.tupleMetadata = buildTupleMetadata( sqm, expectedResultType );
	}

	private static <T> Class<T> determineResultType(SqmSelectStatement<?> sqm, Class<?> expectedResultType) {
		if ( expectedResultType != null && expectedResultType.equals( Tuple.class ) ) {
			//noinspection unchecked
			return (Class<T>) Tuple.class;
		}

		if ( expectedResultType == null || ! expectedResultType.isArray() ) {
			final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
			if ( selections.size() == 1 ) {
				final SqmSelection<?> sqmSelection = selections.get( 0 );
				//noinspection unchecked
				return (Class<T>) sqmSelection.getNodeJavaType().getJavaTypeClass();
			}
		}

		//noinspection unchecked
		return (Class<T>) Object[].class;
	}

	public SqmSelectionQueryImpl(
			NamedHqlQueryMementoImpl memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = memento.getHqlString();
		this.resultType = resultType;

		final SessionFactoryImplementor factory = session.getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final HqlInterpretation hqlInterpretation = interpretationCache.resolveHqlInterpretation(
				hql,
				resultType,
				(s) -> queryEngine.getHqlTranslator().translate( hql, resultType )
		);

		SqmUtil.verifyIsSelectStatement( hqlInterpretation.getSqmStatement(), hql );
		//noinspection unchecked
		this.sqm = (SqmSelectStatement<R>) hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		setComment( hql );
		applyOptions( memento );

		this.tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	public SqmSelectionQueryImpl(
			NamedCriteriaQueryMementoImpl memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		this( (SqmSelectStatement<R>) memento.getSqmStatement(), resultType, session);
		applyOptions( memento );
	}

	public SqmSelectionQueryImpl(
			SqmSelectStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = CRITERIA_HQL_STRING;
		if ( session.isCriteriaCopyTreeEnabled() ) {
			this.sqm = criteria.copy( SqmCopyContext.simpleContext() );
		}
		else {
			this.sqm = criteria;
		}

		this.domainParameterXref = DomainParameterXref.from( sqm );
		if ( ! domainParameterXref.hasParameters() ) {
			this.parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			this.parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : this.domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				final JpaCriteriaParameter<Object> jpaCriteriaParameter = ( (SqmJpaCriteriaParameterWrapper<Object>) sqmParameter ).getJpaCriteriaParameter();
				final Object value = jpaCriteriaParameter.getValue();
				// We don't set a null value, unless the type is also null which is the case when using HibernateCriteriaBuilder.value
				if ( value != null || jpaCriteriaParameter.getNodeType() == null ) {
					// Use the anticipated type for binding the value if possible
					getQueryParameterBindings()
							.getBinding( jpaCriteriaParameter )
							.setBindValue( value, jpaCriteriaParameter.getAnticipatedType() );
				}
			}
		}

		this.resultType = determineResultType( sqm, expectedResultType );
		visitQueryReturnType( sqm.getQueryPart(), expectedResultType, getSessionFactory() );

		setComment( hql );

		this.tupleMetadata = buildTupleMetadata( sqm, expectedResultType );
	}

	public TupleMetadata getTupleMetadata() {
		return tupleMetadata;
	}

	@SuppressWarnings("rawtypes")
	public SqmSelectStatement getSqmStatement() {
		return sqm;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public String getQueryString() {
		return hql;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	@Override
	protected void prepareForExecution() {
		// Reset the callback before every execution
		resetCallback();
	}

	protected List<R> doList() {
		final SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) getSqmStatement();
		final boolean containsCollectionFetches = sqmStatement.containsCollectionFetches();
		final boolean hasLimit = hasLimit( sqmStatement, getQueryOptions() );
		final boolean needsDistinct = containsCollectionFetches
				&& ( sqmStatement.usesDistinct() || hasAppliedGraph( getQueryOptions() ) || hasLimit );

		final DomainQueryExecutionContext executionContextToUse;
		if ( hasLimit && containsCollectionFetches ) {
			boolean fail = getSessionFactory().getSessionFactoryOptions().isFailOnPaginationOverCollectionFetchEnabled();
			if ( fail ) {
				throw new HibernateException(
						"firstResult/maxResults specified with collection fetch. " +
								"In memory pagination was about to be applied. " +
								"Failing because 'Fail on pagination over collection fetch' is enabled."
				);
			}
			else {
				QueryLogging.QUERY_MESSAGE_LOGGER.firstOrMaxResultsSpecifiedWithCollectionFetch();
			}

			final MutableQueryOptions originalQueryOptions = getQueryOptions();
			final QueryOptions normalizedQueryOptions = omitSqlQueryOptions( originalQueryOptions, true, false );
			if ( originalQueryOptions == normalizedQueryOptions ) {
				executionContextToUse = this;
			}
			else {
				executionContextToUse = new DelegatingDomainQueryExecutionContext( this ) {
					@Override
					public QueryOptions getQueryOptions() {
						return normalizedQueryOptions;
					}
				};
			}
		}
		else {
			executionContextToUse = this;
		}

		final List<R> list = resolveQueryPlan().performList( executionContextToUse );

		if ( needsDistinct ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			final int first = !hasLimit || getQueryOptions().getLimit().getFirstRow() == null
					? getIntegerLiteral( sqmStatement.getOffset(), 0 )
					: getQueryOptions().getLimit().getFirstRow();
			final int max = !hasLimit || getQueryOptions().getLimit().getMaxRows() == null
					? getMaxRows( sqmStatement, list.size() )
					: getQueryOptions().getLimit().getMaxRows();
			final List<R> tmp = new ArrayList<>( list.size() );
			final IdentitySet<Object> distinction = new IdentitySet<>( list.size() );
			for ( final R result : list ) {
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			return tmp;
		}
		return list;
	}

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		return resolveQueryPlan().performScroll( scrollMode, this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query plan

	private SelectQueryPlan<R> resolveQueryPlan() {
		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.createInterpretationsKey( this );
		if ( cacheKey != null ) {
			return getSession().getFactory().getQueryEngine().getInterpretationCache().resolveSelectQueryPlan(
					cacheKey,
					this::buildQueryPlan
			);
		}
		else {
			return buildQueryPlan();
		}
	}

	private SelectQueryPlan<R> buildQueryPlan() {
		final SqmSelectStatement<?>[] concreteSqmStatements = QuerySplitter.split(
				(SqmSelectStatement<?>) getSqmStatement(),
				getSession().getFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteQueryPlan( concreteSqmStatements[0], getResultType(), getQueryOptions() );
		}
	}

	private SelectQueryPlan<R> buildAggregatedQueryPlan(SqmSelectStatement<?>[] concreteSqmStatements) {
		//noinspection unchecked
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteQueryPlan(
					concreteSqmStatements[i],
					getResultType(),
					getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	private <T> SelectQueryPlan<T> buildConcreteQueryPlan(
			SqmSelectStatement<?> concreteSqmStatement,
			Class<T> resultType,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				getDomainParameterXref(),
				resultType,
				tupleMetadata,
				queryOptions
		);
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// InterpretationsKeySource

	@Override
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}

	@Override
	public Supplier<Boolean> hasMultiValuedParameterBindingsChecker() {
		return this::hasMultiValuedParameterBindings;
	}

	protected boolean hasMultiValuedParameterBindings() {
		return getQueryParameterBindings().hasAnyMultiValuedBindings()
				|| getParameterMetadata().hasAnyMatching( QueryParameter::allowsMultiValuedBinding );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CommonQueryContract

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	public SqmSelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	/**
	 * Specify the root LockModeType for the query
	 *
	 * @see #setHibernateLockMode
	 */
	@Override
	public SqmSelectionQuery<R> setLockMode(LockModeType lockMode) {
		super.setLockMode( lockMode );
		return this;
	}

	/**
	 * Specify the root {@link LockMode} for the query
	 */
	@Override
	public SqmSelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		super.setHibernateLockMode( lockMode );
		return this;
	}

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 *
	 * @deprecated use {{@link #setLockMode(String, LockMode)}}
	 */
	@Override @Deprecated
	public SqmSelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		super.setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 */
	@Override
	public SqmSelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		super.setLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Specifies whether follow-on locking should be applied?
	 */
	@Override
	public SqmSelectionQuery<R> setFollowOnLocking(boolean enable) {
		getLockOptions().setFollowOnLocking( enable );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheRegion(String regionName) {
		super.setCacheRegion( regionName );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// hints

	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getFetchSize() );

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );

			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph );
		}

		putIfNotNull( hints, HINT_FOLLOW_ON_LOCKING, getQueryOptions().getLockOptions().getFollowOnLocking() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	@Override
	public SqmSelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}


	@Override
	public SqmSelectionQuery<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(int position, Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setProperties(Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}
}
