/*
 * Copyright (c) 2015 Intellectual Reserve, Inc.  All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package cloudfoundry.norouter.f5;

import cloudfoundry.norouter.routingtable.RouteRegisterEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * @author Mike Heath
 */
// TODO Come up with something to reconcile differences in RouterTable and LTM
// TODO Make a sweet README file with architecture diagrams and stuff
// TODO Create BOSH release
public class RouteRegisterListener implements ApplicationListener<RouteRegisterEvent>, Ordered {

	private final Agent agent;

	public RouteRegisterListener(Agent agent) {
		this.agent = agent;
	}

	@Override
	public void onApplicationEvent(RouteRegisterEvent event) {
		agent.registerRoute(event);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
