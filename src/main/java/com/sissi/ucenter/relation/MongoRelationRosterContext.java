package com.sissi.ucenter.relation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sissi.config.MongoConfig;
import com.sissi.config.impl.MongoProxyConfig;
import com.sissi.context.JID;
import com.sissi.context.JIDBuilder;
import com.sissi.protocol.iq.roster.RosterSubscription;
import com.sissi.ucenter.Relation;
import com.sissi.ucenter.RelationContext;
import com.sissi.ucenter.RelationInductor;
import com.sissi.ucenter.RelationRoster;

/**
 * @author kim 2013-11-5
 */
public class MongoRelationRosterContext implements RelationContext {

	private final String fieldAsk = "ask";

	private final String fieldSlave = "slave";

	private final String fieldMaster = "master";

	private final String fieldActivate = "activate";

	private final String fieldGroups = "groups";

	private final Map<String, Object> fieldPlus = Collections.unmodifiableMap(new HashMap<String, Object>());

	private final DBObject filterMaster = BasicDBObjectBuilder.start(this.fieldMaster, 1).get();

	private final DBObject filterSlave = BasicDBObjectBuilder.start(this.fieldSlave, 1).get();

	private final DBObject entityInitMaster = BasicDBObjectBuilder.start(MongoProxyConfig.FIELD_STATE, 0).get();

	private final DBObject entityInitSlave = BasicDBObjectBuilder.start().add(this.fieldActivate, false).add(MongoProxyConfig.FIELD_STATE, 0).get();

	private final DBObject entityEstablishTo = BasicDBObjectBuilder.start("or", 1).get();

	private final DBObject entityEstablishFrom = BasicDBObjectBuilder.start("or", 2).get();

	private final DBObject entityBrokeTo = BasicDBObjectBuilder.start("and", 1).get();

	private final DBObject entityBrokeFrom = BasicDBObjectBuilder.start("and", 2).get();

	private final DBObject[] entityStates = new DBObject[] { BasicDBObjectBuilder.start(MongoProxyConfig.FIELD_STATE, 1).get(), BasicDBObjectBuilder.start(MongoProxyConfig.FIELD_STATE, 3).get() };

	private final MongoConfig config;

	private final JIDBuilder jidBuilder;

	private final Map<String, RelationUpdate> update;

	private RelationInductor relationInductor;

	public MongoRelationRosterContext(MongoConfig config, JIDBuilder jidBuilder) {
		super();
		this.config = config;
		this.jidBuilder = jidBuilder;
		Map<String, RelationUpdate> update = new HashMap<String, RelationUpdate>();
		update.put(RosterSubscription.TO.toString(), new RelationUpdate(this.entityEstablishFrom, this.entityEstablishTo));
		update.put(RosterSubscription.NONE.toString(), new RelationUpdate(this.entityBrokeTo, this.entityBrokeFrom));
		this.update = Collections.unmodifiableMap(update);
	}

	public void setRelationInductor(RelationInductor relationInductor) {
		this.relationInductor = relationInductor;
	}

	private DBObject buildQuery(String master, String slave) {
		return BasicDBObjectBuilder.start().add(this.fieldMaster, master).add(this.fieldSlave, slave).get();
	}

	private DBObject buildQueryWithRole(String role, String jid) {
		return BasicDBObjectBuilder.start().add(role, jid).add(this.fieldActivate, true).get();
	}

	private DBObject buildQueryWithStates(String role, String jid, DBObject[] status) {
		DBObject query = this.buildQueryWithRole(role, jid);
		query.put("$or", status);
		return query;
	}

	@Override
	public MongoRelationRosterContext establish(JID from, Relation relation) {
		this.config.collection().update(this.buildQuery(from.asStringWithBare(), relation.getJID()), BasicDBObjectBuilder.start().add("$set", BasicDBObjectBuilder.start(relation.plus()).add(MongoProxyConfig.FIELD_JID, relation.getName()).add(this.fieldActivate, true).get()).add("$setOnInsert", this.entityInitMaster).get(), true, false, WriteConcern.SAFE);
		this.config.collection().update(this.buildQuery(relation.getJID(), from.asStringWithBare()), BasicDBObjectBuilder.start("$setOnInsert", this.entityInitSlave).get(), true, false, WriteConcern.SAFE);
		return this;
	}

	@Override
	public MongoRelationRosterContext update(JID from, JID to, String state) {
		this.config.collection().update(this.buildQuery(from.asStringWithBare(), to.asStringWithBare()), BasicDBObjectBuilder.start().add("$unset", BasicDBObjectBuilder.start(this.fieldAsk, true).get()).add("$bit", BasicDBObjectBuilder.start().add(MongoProxyConfig.FIELD_STATE, this.update.get(state).getTo()).get()).get(), true, false, WriteConcern.SAFE);
		this.config.collection().update(this.buildQuery(to.asStringWithBare(), from.asStringWithBare()), BasicDBObjectBuilder.start("$bit", BasicDBObjectBuilder.start().add(MongoProxyConfig.FIELD_STATE, this.update.get(state).getFrom()).get()).get(), true, false, WriteConcern.SAFE);
		this.relationInductor.update(to, from);
		return this;
	}

	public MongoRelationRosterContext remove(JID from, JID to) {
		this.config.collection().remove(this.buildQuery(from.asStringWithBare(), to.asStringWithBare()));
		this.config.collection().remove(this.buildQuery(to.asStringWithBare(), from.asStringWithBare()));
		this.relationInductor.remove(to, from);
		return this;
	}

	@Override
	public Set<Relation> myRelations(JID from) {
		return new MongoRelations(this.config.collection().find(this.buildQueryWithRole(this.fieldMaster, from.asStringWithBare())));
	}

	@Override
	public Relation ourRelation(JID from, JID to) {
		DBObject db = this.config.collection().findOne(this.buildQuery(from.asStringWithBare(), to.asStringWithBare()));
		return db != null ? this.build(db) : new NoneRelation(to, RosterSubscription.NONE.toString());
	}

	@Override
	public Set<JID> whoSubscribedMe(JID from) {
		return new JIDGroup(this.config.collection().find(this.buildQueryWithStates(this.fieldSlave, from.asStringWithBare(), this.entityStates), this.filterMaster), this.fieldMaster);
	}

	public Set<JID> iSubscribedWho(JID from) {
		return new JIDGroup(this.config.collection().find(this.buildQueryWithStates(this.fieldMaster, from.asStringWithBare(), this.entityStates), this.filterSlave), this.fieldSlave);
	}

	private Relation build(DBObject db) {
		return new MongoRelationRoster(db);
	}

	private class RelationUpdate {

		private final DBObject from;

		private final DBObject to;

		public RelationUpdate(DBObject from, DBObject to) {
			super();
			this.from = from;
			this.to = to;
		}

		public DBObject getFrom() {
			return this.from;
		}

		public DBObject getTo() {
			return this.to;
		}
	}

	private class JIDGroup extends HashSet<JID> {

		private final static long serialVersionUID = 1L;

		public JIDGroup(DBCursor cursor, String key) {
			if (cursor == null) {
				return;
			}
			while (cursor.hasNext()) {
				this.add(MongoRelationRosterContext.this.jidBuilder.build(MongoRelationRosterContext.this.config.asString(cursor.next(), key)));
			}
		}
	}

	private class MongoRelations extends HashSet<Relation> {

		private final static long serialVersionUID = 1L;

		public MongoRelations(DBCursor cursor) {
			if (cursor == null) {
				return;
			}
			while (cursor.hasNext()) {
				this.add(MongoRelationRosterContext.this.build(cursor.next()));
			}
		}
	}

	private class MongoRelationRoster implements RelationRoster {

		private final String jid;

		private final String name;

		private final Boolean ask;

		private final Boolean activate;

		private final Integer subscription;

		private final String[] groups;

		public MongoRelationRoster(DBObject db) {
			super();
			this.name = MongoRelationRosterContext.this.config.asString(db, MongoProxyConfig.FIELD_NICK);
			this.subscription = MongoRelationRosterContext.this.config.asInteger(db, MongoProxyConfig.FIELD_STATE);
			this.ask = MongoRelationRosterContext.this.config.asBoolean(db, MongoRelationRosterContext.this.fieldAsk);
			this.jid = MongoRelationRosterContext.this.config.asString(db, MongoRelationRosterContext.this.fieldSlave);
			this.groups = MongoRelationRosterContext.this.config.asStrings(db, MongoRelationRosterContext.this.fieldGroups);
			this.activate = MongoRelationRosterContext.this.config.asBoolean(db, MongoRelationRosterContext.this.fieldActivate);
		}

		public String getJID() {
			return this.jid;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean isAsk() {
			return this.ask;
		}

		public String[] asGroups() {
			return this.groups;
		}

		public String getSubscription() {
			return RosterSubscription.toString(this.subscription);
		}

		public boolean in(String... subscriptions) {
			return RosterSubscription.parse(this.getSubscription()).in(subscriptions);
		}

		public boolean in(RosterSubscription... subscriptions) {
			return RosterSubscription.parse(this.getSubscription()).in(subscriptions);
		}

		public boolean isActivate() {
			return this.activate;
		}

		public Map<String, Object> plus() {
			return MongoRelationRosterContext.this.fieldPlus;
		}
	}
}