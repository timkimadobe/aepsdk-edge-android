/*
  Copyright 2020 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.testapp.xdm.commerce;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code SaveForLaters}
 * Product list is saved for future use, for example a product wish list.
 *
 * XDM Property Java Object Generated 2020-10-01 15:22:47.698626 -0700 PDT m=+1.800498671 by XDMTool
 */
@SuppressWarnings("unused")
public class SaveForLaters implements com.adobe.marketing.mobile.xdm.Property {

	private String id;
	private double value;

	public SaveForLaters() {}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();

		if (this.id != null) {
			map.put("id", this.id);
		}

		map.put("value", this.value);

		return map;
	}

	/**
	 * Returns the Unique Identifier property
	 * Unique identifier of the measure. In cases of data collection using lossy communication channels, such as mobile apps or websites with offline functionality, where transmission of measures cannot be ensured, this property contains a client-generated, unique ID of the measure taken. It is best practice to make this sufficiently long to ensure enough entropy. Additionally, if information such as time stamp, device ID, IP, or MAC address, or other potentially user-identifying values are incorporated in the generation of the xdm:id, the result should be hashed, so that no PII is encoded in the value, as the goal is not to identify user or device, but the specific measure in time.
	 * @return {@link String} value or null if the property is not set
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the Unique Identifier property
	 * Unique identifier of the measure. In cases of data collection using lossy communication channels, such as mobile apps or websites with offline functionality, where transmission of measures cannot be ensured, this property contains a client-generated, unique ID of the measure taken. It is best practice to make this sufficiently long to ensure enough entropy. Additionally, if information such as time stamp, device ID, IP, or MAC address, or other potentially user-identifying values are incorporated in the generation of the xdm:id, the result should be hashed, so that no PII is encoded in the value, as the goal is not to identify user or device, but the specific measure in time.
	 * @param newValue the new Unique Identifier value
	 */
	public void setId(final String newValue) {
		this.id = newValue;
	}

	/**
	 * Returns the Value property
	 * The quantifiable value of this measure.
	 * @return double value
	 */
	public double getValue() {
		return this.value;
	}

	/**
	 * Sets the Value property
	 * The quantifiable value of this measure.
	 * @param newValue the new Value value
	 */
	public void setValue(final double newValue) {
		this.value = newValue;
	}
}
