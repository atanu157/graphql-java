package graphql.schema.diffing.ana

import graphql.TestUtil
import graphql.schema.diffing.SchemaDiffing
import spock.lang.Specification

import static graphql.schema.diffing.ana.SchemaChanges.*

class EditOperationAnalyzerTest extends Specification {

    def "test field changed"() {
        given:
        def oldSdl = '''
        type Query {
            hello: String
        }
        '''
        def newSdl = '''
        type Query {
            hello2: String
        }
        '''
        when:
        def changes = changes(oldSdl, newSdl)
        then:
        changes.size() == 1
        (changes[0] as FieldChanged).name == "hello2"
        (changes[0] as FieldChanged).fieldsContainer == "Query"
    }

    def "test field added"() {
        given:
        def oldSdl = '''
        type Query {
            hello: String
        }
        '''
        def newSdl = '''
        type Query {
            hello: String
            newOne: String
        }
        '''
        when:
        def changes = changes(oldSdl, newSdl)
        then:
        changes.size() == 1
        (changes[0] as FieldAdded).name == "newOne"
        (changes[0] as FieldAdded).fieldsContainer == "Query"
    }

    def "test Object added"() {
        given:
        def oldSdl = '''
        type Query {
            hello: String
        }
        '''
        def newSdl = '''
        type Query {
            hello: String
            foo: Foo
        }
        type Foo {
            id: ID
        }
        '''
        when:
        def changes = changes(oldSdl, newSdl)
        def objectAdded = changes.findAll({ it instanceof ObjectAdded }) as List<ObjectAdded>
        then:
        objectAdded.size() == 1
        objectAdded[0].name == "Foo"
    }

    def "test new Interface introduced"() {
        given:
        def oldSdl = '''
        type Query {
            foo: Foo
        }
        type Foo {
          id: ID!
        }
        '''
        def newSdl = '''
        type Query {
            foo: Foo
        }
        type Foo implements Node{
            id: ID!
        }
        interface Node {
            id: ID!
        }
        '''
        when:
        def changes = changes(oldSdl, newSdl)
        def interfaceAdded = changes.findAll({ it instanceof InterfaceAdded }) as List<InterfaceAdded>
        def objectChanged = changes.findAll({ it instanceof ObjectChanged }) as List<ObjectChanged>
        then:
        interfaceAdded.size() == 1
        interfaceAdded[0].name == "Node"
        objectChanged.size() == 1
        objectChanged[0].name == "Foo"
        def addedInterfaceDetails = objectChanged[0].objectChangeDetails.findAll({ it instanceof ObjectChanged.AddedInterfaceObjectChangeDetail }) as List<ObjectChanged.AddedInterfaceObjectChangeDetail>
        addedInterfaceDetails.size() == 1
        addedInterfaceDetails[0].name == "Node"
    }


    List<SchemaChange> changes(
            String oldSdl,
            String newSdl
    ) {
        def oldSchema = TestUtil.schema(oldSdl)
        def newSchema = TestUtil.schema(newSdl)
        def changes = new SchemaDiffing().diffAndAnalyze(oldSchema, newSchema)
        return changes
    }
}
