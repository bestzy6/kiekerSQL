/***************************************************************************
 * Copyright 2021 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.test.tools.junit.log.replayer.teetime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import kieker.common.configuration.Configuration;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.misc.EmptyRecord;
import kieker.common.record.system.MemSwapUsageRecord;
import kieker.monitoring.core.configuration.ConfigurationConstants;
import kieker.monitoring.core.configuration.ConfigurationFactory;

import kieker.tools.log.replayer.AbstractLogReplayer;

import kieker.test.common.junit.AbstractKiekerTest;
import kieker.test.monitoring.util.NamedListWriter;

import teetime.framework.AbstractProducerStage;
import teetime.stage.InitialElementProducer;

/**
 * Tests the {@link AbstractLogReplayer}.
 *
 * @author Andre van Hoorn, Lars Bluemke
 *
 * @since 1.6
 */
public class TestLogReplayer extends AbstractKiekerTest {

	/** A rule making sure that a temporary folder exists for every test method (which is removed after the test). */
	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder(); // NOCS (@Rule must be public)

	private final String listName = TestLogReplayer.class.getName();

	private List<IMonitoringRecord> replayList;
	private File monitoringConfigurationFile;

	public TestLogReplayer() {
		// create test
	}

	/**
	 * Performs an initial test setup.
	 *
	 * @throws IOException
	 *             If the setup failed.
	 */
	@Before
	public void init() throws IOException {
		this.replayList = new ArrayList<IMonitoringRecord>();
		// Adding arbitrary records
		this.replayList.add(new EmptyRecord());
		this.replayList.add(new MemSwapUsageRecord(1, "myHost", 17, // memTotal
				3, // memUsed
				14, // memFree
				100, // swapTotal
				0, // swapUsed
				100 // swapFree
		));
		this.replayList.add(new EmptyRecord());

		// declare configuration
		final Configuration config = ConfigurationFactory.createDefaultConfiguration();
		config.setProperty(ConfigurationConstants.META_DATA, "false");
		config.setProperty(ConfigurationConstants.WRITER_CLASSNAME, NamedListWriter.class.getName());
		config.setProperty(NamedListWriter.CONFIG_PROPERTY_NAME_LIST_NAME, this.listName);

		this.monitoringConfigurationFile = this.tmpFolder.newFile("monitoring.properties");
		final FileOutputStream fos = new FileOutputStream(this.monitoringConfigurationFile);
		try {
			config.store(fos, "Generated by " + TestLogReplayer.class.getName());
		} finally {
			fos.close();
		}
	}

	@Test
	public void testIt() {
		final ListReplayer replayer = new ListReplayer(this.monitoringConfigurationFile.getAbsolutePath(), false, // realtimeMode
				TimeUnit.MILLISECONDS, // realtimeTimeunit
				1.0, // realtimeAccelerationFactor
				true, // keepOriginalLoggingTimestamps
				AbstractLogReplayer.MIN_TIMESTAMP, // ignoreRecordsBeforeTimestamp
				AbstractLogReplayer.MAX_TIMESTAMP, // ignoreRecordsAfterTimestamp
				this.replayList);

		replayer.replay();

		final List<IMonitoringRecord> recordListFilledByListWriter = NamedListWriter.getNamedList(this.listName);
		Assert.assertThat(recordListFilledByListWriter, Matchers.is(this.replayList));
	}

}

/**
 * @author Andre van Hoorn, Lars Bluemke
 *
 * @since 1.6
 */
class ListReplayer extends kieker.tools.log.replayer.teetime.AbstractLogReplayer { // NOPMD
	private final List<IMonitoringRecord> replayList = new ArrayList<IMonitoringRecord>();

	public ListReplayer(final String monitoringConfigurationFile, final boolean realtimeMode,
			final TimeUnit realtimeTimeunit, final double realtimeAccelerationFactor,
			final boolean keepOriginalLoggingTimestamps, final long ignoreRecordsBeforeTimestamp,
			final long ignoreRecordsAfterTimestamp, final List<IMonitoringRecord> replayList) {

		super(monitoringConfigurationFile, realtimeMode, realtimeTimeunit, realtimeAccelerationFactor,
				keepOriginalLoggingTimestamps, ignoreRecordsBeforeTimestamp, ignoreRecordsAfterTimestamp);
		this.replayList.addAll(replayList);
	}

	@Override
	protected AbstractProducerStage<IMonitoringRecord> createReader() {
		return new InitialElementProducer<>(this.replayList);
	}
}
