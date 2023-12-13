/*
 * Copyright 2016 vasgat.
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
package certh.iti.mklab.easie.companymatching;

import com.mongodb.client.MongoCollection;

import java.io.IOException;

import org.bson.types.ObjectId;

/**
 * @author vasgat
 */
public class CompanyMatcher {

    private final CompanyDatabaseActions companyDatabaseActions;
    private final CountryAbreviationsLoader loader = new CountryAbreviationsLoader();
    private String company_name;
    private String website;
    private ObjectId company_id;
    private String country;

    public CompanyMatcher(MongoCollection companies_collection, String company_name, String country, String website) throws IOException {
        this.companyDatabaseActions = new CompanyDatabaseActions(companies_collection);
        buildCompanyMatcher(company_name, country, website);

    }

    private void buildCompanyMatcher(String company_name, String country, String website) {
        this.company_name = company_name.trim();
        if (this.website != null) {
            this.website = website.trim();
        }
        if (country != null) {
            this.country = country.replaceAll("\\.", "");
            if (this.country.trim().length() == 2 && loader.TwoLetterABR.containsKey(this.country.trim())) {
                this.country = loader.TwoLetterABR.get(this.country.trim());
            } else if (this.country.trim().length() == 3 && loader.ThreeLetterABR.containsKey(this.country.trim())) {
                this.country = loader.ThreeLetterABR.get(this.country.trim());
            }
        }

        if (this.website != null && !this.website.equals("-") && !this.website.equals("")) {
            this.company_id = companyDatabaseActions.findCompanyId(this.company_name, this.website, this.country);
        } else {
            this.company_id = companyDatabaseActions.findCompanyId(this.company_name,null, this.country);
        }

        if (company_id == null) {
            company_id = companyDatabaseActions.insertCompany(company_name, website);
        }
        if (country != null) {
            insertInfo("country", this.country);
        }
    }


    /**
     * This method inserts a field with extra infomation for the company in the
     * database
     *
     * @param fieldName
     * @param fieldValue
     */
    public void insertInfo(String fieldName, String fieldValue) {
        companyDatabaseActions.insertInfo(fieldName, fieldValue, company_id);
    }

    /**
     * @returns the company id
     */
    public ObjectId getId() {
        return company_id;
    }

    /**
     * @returns Company's name
     */
    public String getCompanyName() {
        return company_name;
    }

    /**
     * @returns Company's website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Inserts Company to the database based on the available company name and
     * website
     *
     * @return company's id
     */
    private ObjectId insertCompany(String company_name, String website) {
        return companyDatabaseActions.insertCompany(company_name,website);
    }

    /**
     * Searches if the company exists to the database by having available
     * company's website
     *
     * @param CLink company's website
     * @returns company's id if the company exists to database
     */

}
