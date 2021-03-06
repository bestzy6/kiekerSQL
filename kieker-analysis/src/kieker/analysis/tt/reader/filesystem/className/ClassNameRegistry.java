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

package kieker.analysis.tt.reader.filesystem.className;

import java.util.HashMap;

/**
 * @author Christian Wulf
 *
 * @since 1.10
 *
 * @deprecated since 1.15 remove 1.16
 */
@Deprecated
public class ClassNameRegistry extends HashMap<Integer, String> {

	private static final long serialVersionUID = -7254550212115937463L;

	public ClassNameRegistry() {
		// create registry
	}

}
