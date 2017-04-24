/**
 * 
 */
package com.athina.queue.manager.web;

import java.net.URI;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.athina.queue.manager.entity.Job;
import com.athina.queue.manager.entity.JobKey;
import com.athina.queue.manager.entity.JobQueueManager;
import com.athina.queue.manager.entity.JobResponse;

/**
 * @author euro
 *
 */
@RestController
@RequestMapping("/queues/groups")
public class JobQueueController {

	private final JobQueueManager jobQueueManager;

	@Autowired
	public JobQueueController(JobQueueManager scheduler) {
		this.jobQueueManager = scheduler;
	}

	@RequestMapping(value = "/publish/{group}/jobs", method = RequestMethod.POST)
	public ResponseEntity<Job> publish(@PathVariable String group, @RequestBody Job job)
			throws NonMatchingGroupsException, JobQueueManager.DuplicateJobKeyException {
		if (job.getGroup() != null && !job.getGroup().equals(group)) {
			// throwing an exception instead of
			// ResponseEntity.badRequest().build() to have a proper description
			// of the bad request
			throw new NonMatchingGroupsException(group, job.getGroup());
		}
		job.setGroup(group);
		jobQueueManager.scheduleJob(job);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{job}").buildAndExpand(job.getName())
				.toUri();
		return ResponseEntity.created(location).body(job);
	}

	@RequestMapping(value = "/jobkeys", method = RequestMethod.GET)
	public Set<JobKey> getJobKeys() {
		return jobQueueManager.getJobKeys();
	}

	@RequestMapping(value = "/delete/{group}/jobs/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<JobResponse> deleteJob(@PathVariable String group, @PathVariable String name) {
		boolean deleteStatus = jobQueueManager.deleteJob(new JobKey(group, name));
		//ResponseEntity.created(location)
		//return deleteStatus ? 
		return null;
	}

	@RequestMapping(value = "/groups/{group}/jobs", method = RequestMethod.GET)
	public Set<JobKey> getJobKeysByGroup(@PathVariable String group) {
		return jobQueueManager.getJobKeysByGroup(group);
	}

	@RequestMapping(value = "/groups/{group}/jobs/{name}", method = RequestMethod.GET)
	public ResponseEntity<Job> getJob(@PathVariable String group, @PathVariable String name) {
		// since ResponseEntity.notFound().build() returns a
		// ResponseEntity<Void> conflicting with ResponseEntity<Job> we have to
		// use this workaround
		return jobQueueManager.getJob(new JobKey(group, name)).map(ResponseEntity::ok)
				.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	static class NonMatchingGroupsException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NonMatchingGroupsException(String pathGroup, String bodyGroup) {
			super("group provided in the job body should be unspecified or match the path group. "
					+ "Current group in path is '" + pathGroup + "' and in body is '" + bodyGroup + "'.");
		}
	}

}
