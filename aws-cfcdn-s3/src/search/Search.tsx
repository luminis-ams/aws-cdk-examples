import React, {ChangeEvent, Component} from 'react';
import axios from 'axios';

const fetchSearchByTerm = async (term: string): Promise<response> => {
    const { data } = await axios.get(`/prod/?q=${term}`)
    return data;
};

interface response {
    hits: hits | undefined,
}

interface hits {
    hits: Array<hit>
}

interface hit {
    _source: product
}

interface product {
    description: string,
    image_src: string
}

interface ISearchState {
    data: response;
    searchTerm: string;
}

class Search extends Component<{}, ISearchState> {
    state = {
        data: {
            hits: {
                hits: []
            }
        },
        searchTerm: "blauw"
    }

    doSearch = () => {
        fetchSearchByTerm(this.state.searchTerm).then( (response: response) => {
            this.setState({
                data: response
            })
        }).catch((reason => {
            console.log(reason);
        }));
    }

    handleChangeTerm = (event: ChangeEvent) => {
        const target: any = event.target;
        this.setState({ searchTerm: target.value });
    };

    render() {
        return (<div>
            <img src="Luminis-logo-RGB.png" alt="Luminis logo"/>
            <p>Welcome to this very basic demo, enter a color in the search box and push the button</p>
            <div>
                <input id="search_input" onChange={this.handleChangeTerm}/>
                <button onClick={this.doSearch}>Search</button>
            </div>
            <table>
                <tbody>
                {this.state.data && this.state.data.hits && this.state.data.hits.hits && this.state.data.hits.hits.map((hit: any, index: number) => {
                        return (
                            <tr key={index}>
                                <td><img src={hit._source.image_src} alt="dress"/></td>
                                <td>{hit._source.description}</td>
                            </tr>
                        );
                    }
                )}
                </tbody>
            </table>
        </div>);
    }
}

export default Search