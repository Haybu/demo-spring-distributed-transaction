/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agilehandy.remote.handlers;

import java.util.Random;

/**
 * @author Haytham Mohamed
 **/
public class Utilities {

	public static boolean simulateTxn(int max, int lowerBound, int higherBound
			, int maxSecondsToDelay) {
		int randomInteger = new Random().nextInt(max);

		if (randomInteger > lowerBound && randomInteger < higherBound) {
			delay(maxSecondsToDelay);
			return false;
		}

		delay(maxSecondsToDelay);
		return true;
	}

	private static void delay(int maxSecondsToDelay) {
		try {
			int secondsToSleep = new Random().nextInt(maxSecondsToDelay);
			Thread.sleep(secondsToSleep * 1000);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}
