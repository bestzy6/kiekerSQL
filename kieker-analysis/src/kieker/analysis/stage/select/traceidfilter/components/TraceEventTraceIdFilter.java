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

package kieker.analysis.stage.select.traceidfilter.components;

import java.util.Set;

import kieker.common.record.flow.trace.AbstractTraceEvent;

/**
 * Concrete Implementation of {@link AbstractTraceIdFilter}. Allows to filter {@link AbstractTraceEvent}s based on their given trace ids.
 *
 * @author Andre van Hoorn, Jan Waller, Lars Bluemke
 *
 * @since 1.2
 */
public class TraceEventTraceIdFilter extends AbstractTraceIdFilter<AbstractTraceEvent> {

	/**
	 * Create a TraceEventTraceIdFilter stage.
	 *
	 * @param acceptAllTraces
	 *            if true, accept all traces (the filter has no effect)
	 * @param selectedTraceIds
	 *            list of trace ids to accept
	 */
	public TraceEventTraceIdFilter(final boolean acceptAllTraces, final Set<Long> selectedTraceIds) {
		super(acceptAllTraces, selectedTraceIds);
	}

	@Override
	protected long getRecordsTraceId(final AbstractTraceEvent record) {
		return record.getTraceId();
	}

}
