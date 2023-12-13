package certh.iti.mklab.easie.companymatching;

import certh.iti.mklab.easie.MongoUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompanyDatabaseActions {

    private final MongoCollection companies;
    private final CompanySearcher searcher;

    public CompanyDatabaseActions(MongoCollection companies) {

        this.companies = companies;
        this.searcher = new CompanySearcher(companies);
    }

    public void insertInfo(String fieldName, String fieldValue, ObjectId company_id) {
        // Prepare the query for checking existence
        Document existenceQuery = new Document("_id", company_id)
                .append(fieldName.trim(), new Document("$exists", true));

        // Check if the document already exists
        long existingDocumentsCount = companies.count(existenceQuery);

        if (existingDocumentsCount == 0) {
            // Document doesn't exist, perform the update
            Document updateQuery = new Document("_id", company_id);
            Document updateFields = new Document("$set", new Document(fieldName.trim(), fieldValue.trim()));

            companies.updateOne(updateQuery, updateFields, new UpdateOptions().upsert(true));
        }
    }

    public ObjectId insertCompany(String company_name, String website) {
        Document object = new Document();

        object.append("company_name", company_name.trim());
        if (website != null) {
            object.append("website", website.toLowerCase());
        }
        ArrayList list = new ArrayList();
        list.add(company_name.trim());
        object.append("aliases", list);
        ObjectId id = MongoUtils.insertDoc(companies, object);
        return id;
    }

    public ObjectId findCompanyId(String companyName, String cLink, String country) {

        Map<String, String> parameters = new HashMap<>();
        parameters.put("company_name", companyName);
        parameters.put("country", country);

        ObjectId tempId = this.searcher.search(companyName, cLink, country);

        if (tempId == null) {
            parameters.remove("cLink");
            tempId = this.searcher.search(companyName, country);
            if (tempId != null && new Company(tempId, companies).getLink() == null) {
                handleCompanyUpdate(tempId, companyName, cLink.toLowerCase());
            } else {
                tempId = null;
            }
        } else {
            handleCompanyUpdate(tempId, companyName, cLink.toLowerCase());
        }

        return tempId;
    }


    private void handleCompanyUpdate(ObjectId companyId, String companyName, String cLink) {
        MongoCursor<Document> tempCursor = companies
                .find(new Document("_id", companyId))
                .iterator();

        ArrayList aliases = (ArrayList) tempCursor.next().get("aliases");

        if (!aliases.contains(companyName)) {
            aliases.add(companyName);
            Document updateDoc = new Document("$set", new Document("aliases", aliases));
            if (cLink != null) {
                updateDoc.append("$set", new Document("website", cLink));
            }
            companies.updateOne(new Document("_id", companyId), updateDoc);
        }
    }


}
