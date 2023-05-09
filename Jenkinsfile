/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.5') _
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String DEFAULT_JDK_VERSION = '11'
@Field final String DEFAULT_JDK_TOOL = "OpenJDK ${DEFAULT_JDK_VERSION} Latest"
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {
stage('Configure') {
	this.environments = [
//		new BuildEnvironment( dbName: 'h2' ),
//		new BuildEnvironment( dbName: 'hsqldb' ),
//		new BuildEnvironment( dbName: 'derby' ),
//		new BuildEnvironment( dbName: 'mysql' ),
//		new BuildEnvironment( dbName: 'mariadb' ),
//		new BuildEnvironment( dbName: 'postgresql' ),
//		new BuildEnvironment( dbName: 'edb' ),
//		new BuildEnvironment( dbName: 'oracle' ),
//		new BuildEnvironment( dbName: 'db2' ),
//		new BuildEnvironment( dbName: 'mssql' ),
//		new BuildEnvironment( dbName: 'sybase' ),
// Don't build with HANA by default, but only do it nightly until we receive a 3rd instance
// 		new BuildEnvironment( dbName: 'hana_cloud', dbLockableResource: 'hana-cloud', dbLockResourceAsHost: true ),
		new BuildEnvironment( node: 's390x' ),
		new BuildEnvironment( dbName: 'tidb', node: 'tidb',
				additionalOptions: '-DdbHost=localhost:4000',
				notificationRecipients: 'tidb_hibernate@pingcap.com' ),
		new BuildEnvironment( testJdkVersion: '17' ),
		// We want to enable preview features when testing newer builds of OpenJDK:
		// even if we don't use these features, just enabling them can cause side effects
		// and it's useful to test that.
		new BuildEnvironment( testJdkVersion: '19', testJdkLauncherArgs: '--enable-preview' ),
		new BuildEnvironment( testJdkVersion: '20', testJdkLauncherArgs: '--enable-preview' ),
		new BuildEnvironment( testJdkVersion: '21', testJdkLauncherArgs: '--enable-preview' )
	];

	if ( env.CHANGE_ID ) {
		if ( pullRequest.labels.contains( 'cockroachdb' ) ) {
			this.environments.add( new BuildEnvironment( dbName: 'cockroachdb', node: 'cockroachdb', longRunning: true ) )
		}
		if ( pullRequest.labels.contains( 'hana' ) ) {
			this.environments.add( new BuildEnvironment( dbName: 'hana_cloud', dbLockableResource: 'hana-cloud', dbLockResourceAsHost: true ) )
		}
	}

	helper.configure {
		file 'job-configuration.yaml'
		// We don't require the following, but the build helper plugin apparently does
		jdk {
			defaultTool DEFAULT_JDK_TOOL
		}
		maven {
			defaultTool 'Apache Maven 3.8'
		}
	}
	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '30', numToKeepStr: '10')
			),
			// If two builds are about the same branch or pull request,
			// the older one will be aborted when the newer one starts.
			disableConcurrentBuilds(abortPrevious: true),
			helper.generateNotificationProperty()
	])
}

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  print "INFO: Build skipped due to trigger being Branch Indexing"
  currentBuild.result = 'ABORTED'
  return
}

stage('Build') {
	Map<String, Closure> executions = [:]
	Map<String, Map<String, String>> state = [:]
	environments.each { BuildEnvironment buildEnv ->
		// Don't build environments for newer JDKs when this is a PR
		if ( helper.scmSource.pullRequest && buildEnv.testJdkVersion ) {
			return
		}
		state[buildEnv.tag] = [:]
		executions.put(buildEnv.tag, {
			runBuildOnNode(buildEnv.node ?: NODE_PATTERN_BASE) {
				def testJavaHome
				if ( buildEnv.testJdkVersion ) {
					testJavaHome = tool(name: "OpenJDK ${buildEnv.testJdkVersion} Latest", type: 'jdk')
				}
				def javaHome = tool(name: DEFAULT_JDK_TOOL, type: 'jdk')
				// Use withEnv instead of setting env directly, as that is global!
				// See https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md
				withEnv(["JAVA_HOME=${javaHome}", "PATH+JAVA=${javaHome}/bin"]) {
					state[buildEnv.tag]['additionalOptions'] = ''
					if ( testJavaHome ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.version=${buildEnv.testJdkVersion} -Porg.gradle.java.installations.paths=${javaHome},${testJavaHome}"
					}
					if ( buildEnv.testJdkLauncherArgs ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.launcher.args=${buildEnv.testJdkLauncherArgs}"
					}
					state[buildEnv.tag]['containerName'] = null;
					stage('Checkout') {
						checkout scm
					}
					try {
						stage('Start database') {
							switch (buildEnv.dbName) {
								case "h2_1_4":
									state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
										" -Pgradle.libs.versions.h2=1.4.197 -Pgradle.libs.versions.h2gis=1.5.0"
									break;
								case "hsqldb_2_6":
									state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
										" -Pgradle.libs.versions.hsqldb=2.6.1"
									break;
								case "derby_10_14":
									state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
										" -Pgradle.libs.versions.derby=10.14.2.0"
									break;
								case "mysql":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mysql:8.0.31').pull()
									}
									sh "./docker_db.sh mysql"
									state[buildEnv.tag]['containerName'] = "mysql"
									break;
								case "mysql_5_7":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mysql:5.7.40').pull()
									}
									sh "./docker_db.sh mysql_5_7"
									state[buildEnv.tag]['containerName'] = "mysql"
									break;
								case "mariadb":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mariadb:10.9.3').pull()
									}
									sh "./docker_db.sh mariadb"
									state[buildEnv.tag]['containerName'] = "mariadb"
									break;
								case "mariadb_10_3":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mariadb:10.3.36').pull()
									}
									sh "./docker_db.sh mariadb_10_3"
									state[buildEnv.tag]['containerName'] = "mariadb"
									break;
								case "postgresql":
									// use the postgis image to enable the PGSQL GIS (spatial) extension
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('postgis/postgis:15-3.3').pull()
									}
									sh "./docker_db.sh postgresql"
									state[buildEnv.tag]['containerName'] = "postgres"
									break;
								case "postgresql_10":
									// use the postgis image to enable the PGSQL GIS (spatial) extension
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('postgis/postgis:10-2.5').pull()
									}
									sh "./docker_db.sh postgresql_10"
									state[buildEnv.tag]['containerName'] = "postgres"
									break;
								case "edb":
									docker.image('quay.io/enterprisedb/edb-postgres-advanced:15.2-3.3-postgis').pull()
									sh "./docker_db.sh edb"
									state[buildEnv.tag]['containerName'] = "edb"
									break;
								case "edb_10":
									docker.image('quay.io/enterprisedb/edb-postgres-advanced:10.22').pull()
									sh "./docker_db.sh edb_10"
									state[buildEnv.tag]['containerName'] = "edb"
									break;
								case "oracle":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('gvenzl/oracle-xe:21.3.0-full').pull()
									}
									sh "./docker_db.sh oracle"
									state[buildEnv.tag]['containerName'] = "oracle"
									break;
								case "oracle_11_2":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('gvenzl/oracle-xe:11.2.0.2-full').pull()
									}
									sh "./docker_db.sh oracle_11"
									state[buildEnv.tag]['containerName'] = "oracle"
									break;
								case "db2":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('ibmcom/db2:11.5.7.0').pull()
									}
									sh "./docker_db.sh db2"
									state[buildEnv.tag]['containerName'] = "db2"
									break;
								case "db2_10_5":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('ibmoms/db2express-c@sha256:a499afd9709a1f69fb41703e88def9869955234c3525547e2efc3418d1f4ca2b').pull()
									}
									sh "./docker_db.sh db2_10_5"
									state[buildEnv.tag]['containerName'] = "db2"
									break;
								case "mssql":
									docker.image('mcr.microsoft.com/mssql/server@sha256:f54a84b8a802afdfa91a954e8ddfcec9973447ce8efec519adf593b54d49bedf').pull()
									sh "./docker_db.sh mssql"
									state[buildEnv.tag]['containerName'] = "mssql"
									break;
								case "mssql_2017":
									docker.image('mcr.microsoft.com/mssql/server@sha256:7d194c54e34cb63bca083542369485c8f4141596805611e84d8c8bab2339eede').pull()
									sh "./docker_db.sh mssql_2017"
									state[buildEnv.tag]['containerName'] = "mssql"
									break;
								case "sybase":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('nguoianphu/docker-sybase').pull()
									}
									sh "./docker_db.sh sybase"
									state[buildEnv.tag]['containerName'] = "sybase"
									break;
								case "cockroachdb":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('cockroachdb/cockroach:v22.2.2').pull()
									}
									sh "./docker_db.sh cockroachdb"
									state[buildEnv.tag]['containerName'] = "cockroach"
									break;
								case "cockroachdb_21_2":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('cockroachdb/cockroach:v21.2.16').pull()
									}
									sh "./docker_db.sh cockroachdb_21_2"
									state[buildEnv.tag]['containerName'] = "cockroach"
									break;
							}
						}
						stage('Test') {
							String cmd = "./ci/build.sh ${buildEnv.additionalOptions ?: ''} ${state[buildEnv.tag]['additionalOptions'] ?: ''}"
							withEnv(["RDBMS=${buildEnv.dbName}"]) {
								try {
									if (buildEnv.dbLockableResource == null) {
										timeout( [time: buildEnv.longRunning ? 480 : 120, unit: 'MINUTES'] ) {
											sh cmd
										}
									}
									else {
										lock(label: buildEnv.dbLockableResource, quantity: 1, variable: 'LOCKED_RESOURCE') {
											if ( buildEnv.dbLockResourceAsHost ) {
												cmd += " -DdbHost=${LOCKED_RESOURCE}"
											}
											timeout( [time: buildEnv.longRunning ? 480 : 120, unit: 'MINUTES'] ) {
												sh cmd
											}
										}
									}
								}
								finally {
									junit '**/target/test-results/test/*.xml,**/target/test-results/testKitTest/*.xml'
								}
							}
						}
					}
					finally {
						if ( state[buildEnv.tag]['containerName'] != null ) {
							sh "docker rm -f ${state[buildEnv.tag]['containerName']}"
						}
						// Skip this for PRs
						if ( !env.CHANGE_ID && buildEnv.notificationRecipients != null ) {
							handleNotifications(currentBuild, buildEnv)
						}
					}
				}
			}
		})
	}
	parallel(executions)
}

} // End of helper.runWithNotification

// Job-specific helpers

class BuildEnvironment {
	String testJdkVersion
	String testJdkLauncherArgs
	String dbName = 'h2'
	String node
	String dbLockableResource
	boolean dbLockResourceAsHost
	String additionalOptions
	String notificationRecipients
	boolean longRunning

	String toString() { getTag() }
	String getTag() { "${node ? node + "_" : ''}${testJdkVersion ? 'jdk_' + testJdkVersion + '_' : '' }${dbName}" }
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        try {
			body()
        }
        finally {
        	// If this is a PR, we clean the workspace at the end
        	if ( env.CHANGE_BRANCH != null ) {
        		cleanWs()
        	}
        	pruneDockerContainers()
        }
	}
}
void pruneDockerContainers() {
	if ( !sh( script: 'command -v docker || true', returnStdout: true ).trim().isEmpty() ) {
		sh 'docker container prune -f || true'
		sh 'docker image prune -f || true'
		sh 'docker network prune -f || true'
		sh 'docker volume prune -f || true'
	}
}

void handleNotifications(currentBuild, buildEnv) {
	def currentResult = getParallelResult(currentBuild, buildEnv.tag)
	boolean success = currentResult == 'SUCCESS' || currentResult == 'UNKNOWN'
	def previousResult = currentBuild.previousBuild == null ? null : getParallelResult(currentBuild.previousBuild, buildEnv.tag)

	// Ignore success after success
	if ( !( success && previousResult == 'SUCCESS' ) ) {
		def subject
		def body
		if ( success ) {
			if ( previousResult != 'SUCCESS' && previousResult != null ) {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Fixed"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Fixed:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
			else {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Success"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Success:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
		}
		else if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
			// If there are interrupted build actions, this means the build was cancelled, probably superseded
			// Thanks to https://issues.jenkins.io/browse/JENKINS-43339 for the "hack" to determine this
			if ( currentResult == 'FAILURE' ) {
				if ( previousResult != null && previousResult == "FAILURE" ) {
					subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Still failing"
					body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Still failing:</p>
						<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
				}
				else {
					subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Failure"
					body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Failure:</p>
						<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
				}
			}
			else {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - ${currentResult}"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - ${currentResult}:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
		}

		emailext(
				subject: subject,
				body: body,
				to: buildEnv.notificationRecipients
		)
	}
}

@NonCPS
String getParallelResult( RunWrapper build, String parallelBranchName ) {
    def visitor = new PipelineNodeGraphVisitor( build.rawBuild )
    def branch = visitor.pipelineNodes.find{ it.type == FlowNodeWrapper.NodeType.PARALLEL && parallelBranchName == it.displayName }
    if ( branch == null ) {
    	echo "Couldn't find parallel branch name '$parallelBranchName'. Available parallel branch names:"
		visitor.pipelineNodes.findAll{ it.type == FlowNodeWrapper.NodeType.PARALLEL }.each{
			echo " - ${it.displayName}"
		}
    	return null;
    }
    return branch.status.result
}