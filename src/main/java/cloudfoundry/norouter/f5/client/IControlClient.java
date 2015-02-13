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
package cloudfoundry.norouter.f5.client;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface IControlClient extends AutoCloseable {
	@Override
	void close();

	Pool createPool(Pool pool);

	Pool getPool(String name);

	void deletePool(String name);

	Collection<Pool> getAllPools();

	Collection<Pool> getAllPools(boolean fetchPoolMembers);

	void addPoolMember(String poolName, InetSocketAddress poolMember);

	void addPoolMember(String poolName, InetSocketAddress poolMember, String description);

	void addPoolMember(String poolName, String poolMember);

	PoolMember addPoolMember(String poolName, String poolMember, String description);

	void deletePoolMember(String poolName, InetSocketAddress poolMember);

	void deletePoolMember(String poolName, String poolMember);
}
