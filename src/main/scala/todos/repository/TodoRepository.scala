package todos.repository

import todos.models.{TodoItem, UpdateTodoRequest}
import zio.Task
import zio.*
import doobie.*
import doobie.implicits.*
import zio.interop.catz.*
import doobie.postgres.implicits.*

import java.util.UUID

trait TodoRepository {
		def getAllByUserId(userId: UUID): Task[List[TodoItem]]

		def getById(id: UUID): Task[Option[TodoItem]]

		def crateTodoItem(item: TodoItem): Task[Unit]

		def updateTodoItem(id: UUID, updateRequest: UpdateTodoRequest): Task[Unit]

		def deleteTodoItem(id: UUID): Task[Unit]
}

class PostgresTodoRepository(xa: Transactor[Task]) extends TodoRepository {

		override def getAllByUserId(userId: UUID): Task[List[TodoItem]] =
				sql"""
					SELECT user_id, id, description, priority, is_complete, created_at, completed_at, tags
        FROM todo_items WHERE user_id = $userId
        """
				.query[TodoItem]
				.to[List]
				.transact(xa)

		override def getById(id: UUID): Task[Option[TodoItem]] =
				sql"""
						SELECT user_id, id, description, priority, is_complete, created_at, completed_at, tags
						FROM todo_items WHERE id = $id
					"""
				.query[TodoItem]
				.option
				.transact(xa)

		override def crateTodoItem(item: TodoItem): Task[Unit] =
				sql"""
					INSERT INTO todo_items(
					user_id,
					id,
					description,
					priority,
					is_complete,
					created_at,
					completed_at,
					tags
					) VALUES (
					${item.userId},
					${item.id},
					${item.description},
          ${item.priority},
          ${item.isComplete},
          ${item.createAt},
          ${item.completeAt},
          ${item.tags}
					)
					"""
				.update
				.run
				.transact(xa)
				.unit
		
		override def updateTodoItem(id: UUID, updateRequest: UpdateTodoRequest): Task[Unit] = {
				
				val updates = List(
						updateRequest.description.map(d => fr"description = $d"),
						updateRequest.priority.map(p => fr"priority = $p"),
						updateRequest.isComplete.map(c => fr"is_complete = $c"),
						updateRequest.completeAt.map(ca => fr"completed_at = $ca"),
						updateRequest.tags.map(t => fr"tags = $t")
				).collect { case Some(fragment) => fragment }
				
				if (updates.isEmpty) {
						ZIO.unit
				}
				else {
						val setClause = updates.reduce(_ ++ fr"," ++ _)
						
						(fr"UPDATE todo_items SET" ++ setClause ++ fr"WHERE id = $id")
						.update
						.run
						.transact(xa)
						.unit
				}
		}

		override def deleteTodoItem(id: UUID): Task[Unit] =
				sql"""DELETE FROM todo_items WHERE id = $id"""
				.update
				.run
				.transact(xa)
				.unit
}