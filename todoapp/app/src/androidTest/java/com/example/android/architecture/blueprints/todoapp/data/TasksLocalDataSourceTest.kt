/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.data

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.util.any
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

/**
 * Integration test for the [TasksDataSource], which uses the [TasksDbHelper].
 */
@RunWith(AndroidJUnit4::class) @LargeTest class TasksLocalDataSourceTest {

    private val TITLE = "title"
    private val TITLE2 = "title2"
    private val TITLE3 = "title3"
    private lateinit var localDataSource: TasksLocalDataSource

    @Before fun setup() {
        localDataSource = TasksLocalDataSource.getInstance(
                InstrumentationRegistry.getTargetContext())
    }

    @After fun cleanUp() {
        localDataSource.deleteAllTasks()
    }

    @Test fun testPreConditions() {
        assertNotNull(localDataSource)
    }

    @Test fun saveTask_retrievesTask() = runBlocking<Unit> {
        // Given a new task
        val newTask = Task(TITLE)

        with(localDataSource) {
            // When saved into the persistent repository
            saveTask(newTask)

            // Then the task can be retrieved from the persistent repository
            val task = getTask(newTask.id)
            assertThat(task, `is`(newTask))
        }
    }

    @Test fun completeTask_retrievedTaskIsComplete() = runBlocking<Unit> {
        // Given a new task in the persistent repository
        val newTask = Task(TITLE)
        with(localDataSource) {
            saveTask(newTask)

            // When completed in the persistent repository
            completeTask(newTask)

            // Then the task can be retrieved from the persistent repository and is complete
            val task = getTask(newTask.id)
            assertThat(task, `is`(newTask))
            assertThat(task!!.isCompleted, `is`(true))
        }
    }

    @Test fun activateTask_retrievedTaskIsActive() = runBlocking<Unit> {
        // Given a new completed task in the persistent repository
        val newTask = Task(TITLE)
        with(localDataSource) {
            saveTask(newTask)
            completeTask(newTask)

            // When activated in the persistent repository
            activateTask(newTask)

            // Then the task can be retrieved from the persistent repository and is active
            assertThat(getTask(newTask.id), IsEqual(newTask))
            assertThat(newTask.isCompleted, `is`(false))
        }
    }

    @Test fun clearCompletedTask_taskNotRetrievable() = runBlocking<Unit> {
        // Given 2 new completed tasks and 1 active task in the persistent repository
        val newTask1 = Task(TITLE)
        val newTask2 = Task(TITLE2)
        val newTask3 = Task(TITLE3)

        with(localDataSource) {
            saveTask(newTask1)
            completeTask(newTask1)
            saveTask(newTask2)
            completeTask(newTask2)
            saveTask(newTask3)
            // When completed tasks are cleared in the repository
            clearCompletedTasks()

            // Then the completed tasks cannot be retrieved and the active one can
            assertThat(getTask(newTask1.id), IsNull())

            assertThat(getTask(newTask2.id), IsNull())

            assertThat(getTask(newTask3.id), IsEqual(newTask3))
        }
    }

    @Test fun deleteAllTasks_emptyListOfRetrievedTask() {
        val callback = mock(TasksDataSource.LoadTasksCallback::class.java)

        // Given a new task in the persistent repository and a mocked callback
        val newTask = Task(TITLE)

        with(localDataSource) {
            saveTask(newTask)

            // When all tasks are deleted
            deleteAllTasks()

            // Then the retrieved tasks is an empty list
            getTasks(callback)
        }
        verify<TasksDataSource.LoadTasksCallback>(callback).onDataNotAvailable()
        verify<TasksDataSource.LoadTasksCallback>(callback, never())
                .onTasksLoaded(any<List<Task>>())
    }

    @Test fun getTasks_retrieveSavedTasks() {
        // Given 2 new tasks in the persistent repository
        val newTask1 = Task(TITLE)
        val newTask2 = Task(TITLE)

        with(localDataSource) {
            saveTask(newTask1)
            saveTask(newTask2)
            // Then the tasks can be retrieved from the persistent repository
            getTasks(object : TasksDataSource.LoadTasksCallback {
                override fun onTasksLoaded(tasks: List<Task>) {
                    assertNotNull(tasks)
                    assertTrue(tasks.size >= 2)

                    var newTask1IdFound = false
                    var newTask2IdFound = false
                    for ((_, _, id) in tasks) {
                        if (id == newTask1.id) {
                            newTask1IdFound = true
                        }
                        if (id == newTask2.id) {
                            newTask2IdFound = true
                        }
                    }
                    assertTrue(newTask1IdFound)
                    assertTrue(newTask2IdFound)
                }

                override fun onDataNotAvailable() {
                    fail()
                }
            })
        }
    }
}
