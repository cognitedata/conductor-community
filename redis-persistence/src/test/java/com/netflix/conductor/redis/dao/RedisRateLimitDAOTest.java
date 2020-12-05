/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.redis.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.config.ObjectMapperConfiguration;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.redis.jedis.JedisProxy;
import com.netflix.conductor.redis.config.RedisProperties;
import com.netflix.conductor.redis.jedis.JedisMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.commands.JedisCommands;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = {ObjectMapperConfiguration.class})
@RunWith(SpringRunner.class)
public class RedisRateLimitDAOTest {

    private RedisRateLimitingDAO rateLimitingDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void init() {
        RedisProperties properties = mock(RedisProperties.class);
        JedisCommands jedisMock = new JedisMock();
        JedisProxy jedisProxy = new JedisProxy(jedisMock);

        rateLimitingDao = new RedisRateLimitingDAO(jedisProxy, objectMapper, properties);
    }

    @Test
    public void testExceedsRateLimitWhenNoRateLimitSet() {
        TaskDef taskDef = new TaskDef("TestTaskDefinition");
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTaskDefName(taskDef.getName());
        assertFalse(rateLimitingDao.exceedsRateLimitPerFrequency(task, taskDef));
    }

    @Test
    public void testExceedsRateLimitWithinLimit() {
        TaskDef taskDef = new TaskDef("TestTaskDefinition");
        taskDef.setRateLimitFrequencyInSeconds(60);
        taskDef.setRateLimitPerFrequency(20);
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTaskDefName(taskDef.getName());
        assertFalse(rateLimitingDao.exceedsRateLimitPerFrequency(task, taskDef));
    }

    @Test
    public void testExceedsRateLimitOutOfLimit() {
        TaskDef taskDef = new TaskDef("TestTaskDefinition");
        taskDef.setRateLimitFrequencyInSeconds(60);
        taskDef.setRateLimitPerFrequency(1);
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTaskDefName(taskDef.getName());
        assertFalse(rateLimitingDao.exceedsRateLimitPerFrequency(task, taskDef));
        assertTrue(rateLimitingDao.exceedsRateLimitPerFrequency(task, taskDef));
    }
}
