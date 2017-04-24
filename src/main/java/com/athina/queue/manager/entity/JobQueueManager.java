/*
 * Copyright (C) 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.athina.queue.manager.entity;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;


@Service
public class JobQueueManager {

  private static final Logger LOG = LoggerFactory.getLogger(JobQueueManager.class);

  private final org.quartz.Scheduler quartzScheduler;

  @Autowired
  public JobQueueManager(org.quartz.Scheduler quartzScheduler) {
    this.quartzScheduler = quartzScheduler;
  }

  public void scheduleJob(@Valid Job job) throws DuplicateJobKeyException {
    Set<org.quartz.Trigger> quartzTriggers = job.buildQuartzTriggers();
    JobDetail quartzJobDetail = job.buildQuartzJobDetail();
    try {
      quartzScheduler.scheduleJob(quartzJobDetail, quartzTriggers, false);
      LOG.info("Scheduled: {}", job);
    } catch (ObjectAlreadyExistsException e) {
      throw new DuplicateJobKeyException(job.getGroup(), job.getName(), e);
    } catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  public static class DuplicateJobKeyException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DuplicateJobKeyException(String group, String name, ObjectAlreadyExistsException e) {
      super("already exists a job in group '" + group + "' with name '" + name + "'.", e);
    }
  }

  public Set<JobKey> getJobKeys() {
    try {
      return quartzScheduler.getJobKeys(GroupMatcher.anyJobGroup())
          .stream()
          .map(JobKey::fromQuartzJobKey)
          .collect(Collectors.toSet());
    } catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  public Set<JobKey> getJobKeysByGroup(String group) {
    try {
      return quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))
          .stream()
          .map(JobKey::fromQuartzJobKey)
          .collect(Collectors.toSet());
    } catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<Job> getJob(JobKey jobKey) {
    try {
      JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey.buildQuartzJobKey());

      if (jobDetail == null) {
        return Optional.empty();
      }

      Set<org.quartz.Trigger> quartzTriggers =
          ImmutableSet.copyOf(quartzScheduler.getTriggersOfJob(jobDetail.getKey()));
      return Optional.of(Job.fromQuartzJobDetailAndTriggers(jobDetail, quartzTriggers));
    } catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  public boolean deleteJob(JobKey jobKey) {
    try {
      return quartzScheduler.deleteJob(jobKey.buildQuartzJobKey());
    } catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

}
