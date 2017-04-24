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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.hibernate.validator.constraints.NotEmpty;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = HttpJob.class, name = "http")})
public abstract class Job implements org.quartz.Job, Runnable {

  private static final String ID_DATAMAP_KEY = "id";
  /* This id is manly for tracing because a job could be created and another could use same name
     and group afterwards, but with this id both jobs will have different ids */
  protected UUID id;
  protected String group;
  protected String name;
  @NotEmpty
  @Valid
  protected Set<Trigger> triggers;

  public Job() {
    id = UUID.randomUUID();
  }

  @SuppressWarnings("UnusedDeclaration")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @SuppressWarnings("UnusedDeclaration")
  public Set<Trigger> getTriggers() {
    return triggers;
  }

  public void setTriggers(Set<Trigger> triggers) {
    this.triggers = triggers;
  }

  public Set<org.quartz.Trigger> buildQuartzTriggers() {
    return triggers.stream().
        map(Trigger::buildQuartzTrigger).
        collect(Collectors.toSet());
  }

  public JobDetail buildQuartzJobDetail() {
    JobDataMap dataMap = new JobDataMap();
    dataMap.put(ID_DATAMAP_KEY, id.toString());
    dataMap.putAll(buildDataMap());

    return org.quartz.JobBuilder.newJob(getClass())
        .withIdentity(name, group)
        .usingJobData(dataMap)
        .build();
  }

  protected abstract Map<String, Object> buildDataMap();

  public static Job fromQuartzJobDetailAndTriggers(JobDetail jobDetail,
                                                   Set<? extends org.quartz.Trigger> triggers) {
    try {
      Job job = (Job) jobDetail.getJobClass().newInstance();
      org.quartz.JobKey jobKey = jobDetail.getKey();
      job.setId(UUID.fromString((String) jobDetail.getJobDataMap().remove(ID_DATAMAP_KEY)));
      job.setName(jobKey.getName());
      job.setGroup(jobKey.getGroup());
      job.setTriggers(triggers.stream()
                          .map(Trigger::fromQuartzTrigger)
                          .collect(Collectors.toSet()));
      job.initFromDataMap(jobDetail.getJobDataMap());
      return job;
    } catch (InstantiationException | IllegalAccessException e) {
      throw Throwables.propagate(e);
    }
  }

  protected abstract void initFromDataMap(Map<String, Object> dataMap);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    run();
  }

}
