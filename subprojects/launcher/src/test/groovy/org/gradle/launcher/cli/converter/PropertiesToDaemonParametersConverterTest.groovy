/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.cli.converter

import org.gradle.initialization.BuildLayoutParameters
import org.gradle.internal.jvm.Jvm
import org.gradle.launcher.daemon.configuration.DaemonBuildOptionFactory
import org.gradle.launcher.daemon.configuration.DaemonParameters
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.UsesNativeServices
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

@UsesNativeServices
class PropertiesToDaemonParametersConverterTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()

    def converter = new PropertiesToDaemonParametersConverter()
    def params = new DaemonParameters(new BuildLayoutParameters())

    def "allows whitespace around boolean properties"() {
        when:
        converter.convert([ (DaemonBuildOptionFactory.DaemonOption.GRADLE_PROPERTY): 'false ' ], params)
        then:
        !params.enabled
    }

    def "can configure jvm args combined with a system property"() {
        when:
        converter.convert([(DaemonBuildOptionFactory.JvmArgsOption.GRADLE_PROPERTY): '-Xmx512m -Dprop=value'], params)

        then:
        params.effectiveJvmArgs.contains('-Xmx512m')
        !params.effectiveJvmArgs.contains('-Dprop=value')

        params.systemProperties == [prop: 'value']
    }

    def "supports 'empty' system properties"() {
        when:
        converter.convert([(DaemonBuildOptionFactory.JvmArgsOption.GRADLE_PROPERTY): "-Dfoo= -Dbar"], params)

        then:
        params.systemProperties == [foo: '', bar: '']
    }

    def "configures from gradle properties"() {
        when:
        converter.convert([
            (DaemonBuildOptionFactory.JvmArgsOption.GRADLE_PROPERTY)     : '-Xmx256m',
            (DaemonBuildOptionFactory.JavaHomeOption.GRADLE_PROPERTY)    : Jvm.current().javaHome.absolutePath,
            (DaemonBuildOptionFactory.DaemonOption.GRADLE_PROPERTY)      : "false",
            (DaemonBuildOptionFactory.BaseDirOption.GRADLE_PROPERTY)     : new File("baseDir").absolutePath,
            (DaemonBuildOptionFactory.IdleTimeoutOption.GRADLE_PROPERTY) : "115",
            (DaemonBuildOptionFactory.HealthCheckOption.GRADLE_PROPERTY) : "42",
            (DaemonBuildOptionFactory.DebugOption.GRADLE_PROPERTY)       : "true",
        ], params)

        then:
        params.effectiveJvmArgs.contains("-Xmx256m")
        params.debug
        params.effectiveJvm == Jvm.current()
        !params.enabled
        params.baseDir == new File("baseDir").absoluteFile
        params.idleTimeout == 115
        params.periodicCheckInterval == 42
    }

    def "shows nice message for dummy java home"() {
        when:
        converter.convert([(DaemonBuildOptionFactory.JavaHomeOption.GRADLE_PROPERTY): "/invalid/path"], params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains 'org.gradle.java.home'
        ex.message.contains '/invalid/path'
    }

    def "shows nice message for invalid java home"() {
        def dummyDir = temp.createDir("foobar")
        when:
        converter.convert([(DaemonBuildOptionFactory.JavaHomeOption.GRADLE_PROPERTY): dummyDir.absolutePath], params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains 'org.gradle.java.home'
        ex.message.contains 'foobar'
    }

    def "shows nice message for invalid idle timeout"() {
        when:
        converter.convert((DaemonBuildOptionFactory.IdleTimeoutOption.GRADLE_PROPERTY): 'asdf', params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains 'org.gradle.daemon.idletimeout'
        ex.message.contains 'asdf'
    }

    def "shows nice message for invalid periodic check interval"() {
        when:
        converter.convert((DaemonBuildOptionFactory.HealthCheckOption.GRADLE_PROPERTY): 'bogus', params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains 'org.gradle.daemon.healthcheckinterval'
        ex.message.contains 'bogus'
    }

    @Unroll
    def "explicitly sets daemon usage if daemon system property is specified"() {
        when:
        converter.convert((DaemonBuildOptionFactory.DaemonOption.GRADLE_PROPERTY): enabled.toString(), params)

        then:
        params.enabled == propertyValue

        where:
        enabled | propertyValue
        true    | true
        false   | false
    }

    def "enable debug mode from JVM args when default debug argument is used"() {
        when:
        converter.convert([
            (DaemonBuildOptionFactory.JvmArgsOption.GRADLE_PROPERTY)                 : "-Xmx256m $debugArgs".toString(),
        ], params)

        then:
        params.debug

        where:
        debugArgs << ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005', '-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005']
    }

}
